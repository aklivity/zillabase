#
# Copyright 2024 Aklivity Inc
#
# Licensed under the Aklivity Community License (the "License"); you may not use
# this file except in compliance with the License.  You may obtain a copy of the
# License at
#
#   https://www.aklivity.io/aklivity-community-license/
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OF ANY KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations under the License.
#

import importlib.metadata
import importlib.util
import inspect
import os
import re
import subprocess
import sys
from pathlib import Path

from flask import Flask, jsonify
from threading import Thread

from arrow_udf import UdfServer

CLASS_NAME_PATTERN = re.compile(r"([a-z])([A-Z])")

app = Flask(__name__)

def fetch_classes():
    class_names = []
    classpath = os.environ.get("PYTHONPATH", None)
    if classpath:
        paths = classpath.split(os.pathsep)
        for path in paths:
            if path.startswith("/opt/udf/lib"):
                path_obj = Path(path)
                if path_obj.is_dir():
                    for file in path_obj.rglob("*.py"):
                        class_names.append(file)
    return class_names

def install_package(package_name, version=None):
    package_str = f"{package_name}=={version}" if version else package_name
    subprocess.check_call([sys.executable, "-m", "pip", "install", package_str])

def ensure_packages_installed(packages):
    for package_name, version in packages.items():
        try:
            installed_version = importlib.metadata.version(package_name)
            if version:
                if installed_version == version:
                    print(f"{package_name} {version} is already installed.")
                else:
                    print(f"Package '{package_name}' found with version {installed_version}, "
                          f"but {version} is required. Installing the correct version...")
                    install_package(package_name, version)
            else:
                print(f"{package_name} is already installed.")
        except importlib.metadata.PackageNotFoundError:
            print(f"Package '{package_name}=={version}' not found. Installing...")
            install_package(package_name, version)

def get_requirements_packages(requirements_path):
    packages = {}
    if os.path.exists(requirements_path):
        with open(requirements_path, "r") as req_file:
            for line in req_file:
                line = line.strip()
                if line and not line.startswith("#"):
                    if "==" in line:
                        package, version = line.split("==")
                        packages[package] = version
                    else:
                        packages[line] = None
    return packages

def dynamic_import(file_path):
    module_name = file_path.stem
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

@app.route("/python/methods", methods=["GET"])
def list_methods():
    all_functions_info = []

    for f in functions:
        input_schema_str = str(getattr(f, "_input_schema", ""))
        result_schema_str = str(getattr(f, "_result_schema", ""))

        input_pairs = []
        for line in input_schema_str.splitlines():
            line = line.strip()
            if not line:
                continue

            if ": " in line:
                left, right = line.split(": ", 1)
                field_name = left.strip()
                field_type = right.strip()

                if field_name == f._name:
                    field_name = ""

                input_pairs.append({
                    "name": field_name,
                    "type": field_type
                })

        result_pairs = []
        for line in result_schema_str.splitlines():
            line = line.strip()
            if not line or line.startswith("child"):
                continue

            if ": " in line:
                left, right = line.split(": ", 1)
                field_name = left.strip()
                field_type = right.strip()

                if field_name == f._name:
                    field_name = ""

                result_pairs.append({
                    "name": field_name,
                    "type": field_type
                })

        function_info = {
            "name": f._name,
            "input_type": input_pairs,
            "result_type": result_pairs
        }
        all_functions_info.append(function_info)

    return jsonify(all_functions_info)

def start_flask_server():
    # Start the Flask server on port 5000 in a separate thread
    app.run(host="0.0.0.0", port=5000)


if __name__ == "__main__":
    functions = []
    matcher = CLASS_NAME_PATTERN

    try:
        requirements_file_path = "/opt/udf/lib/requirements.txt"
        requirements_packages = get_requirements_packages(requirements_file_path)
        ensure_packages_installed(requirements_packages)

        class_files = fetch_classes()
        for class_file in class_files:
            try:
                module = dynamic_import(class_file)
                print(f"Module loaded: {class_file}")

                for name, function in inspect.getmembers(module):
                    if (hasattr(function, '__module__') and
                            function.__module__ == 'arrow_udf' and
                            hasattr(function, '_name')):
                        functions.append(function)

            except (ModuleNotFoundError, AttributeError, ImportError) as ex:
                print(f"Error loading {class_file}: {ex}")

        # Start up your web server for listing methods:
        flask_thread = Thread(target=start_flask_server, daemon=True)
        flask_thread.start()

         #Start up the Arrow UDF server:
        server = UdfServer(location="0.0.0.0:8816")
        for function in functions:
            server.add_function(function)
        server.serve()

    except Exception as ex:
        print(f"An error occurred: {ex}")
