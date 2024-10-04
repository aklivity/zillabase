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

import importlib.util
import inspect
import os
import re
from pathlib import Path
from arrow_udf import UdfServer

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

def dynamic_import(file_path):
    module_name = file_path.stem
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

CLASS_NAME_PATTERN = re.compile(r"([a-z])([A-Z])")

if __name__ == "__main__":
    server = UdfServer(location="0.0.0.0:8816")
    matcher = CLASS_NAME_PATTERN

    try:
        class_files = fetch_classes()
        for class_file in class_files:
            try:
                module = dynamic_import(class_file)
                print(f"Module loaded: {class_file}")

                for name, function in inspect.getmembers(module):
                    if (hasattr(function, '__module__') and
                            function.__module__ == 'arrow_udf' and
                            hasattr(function, '_name')):
                        server.add_function(function)

            except (ModuleNotFoundError, AttributeError, ImportError) as ex:
                print(f"Error loading {class_file}: {ex}")

        server.serve()

    except Exception as ex:
        print(f"An error occurred: {ex}")
