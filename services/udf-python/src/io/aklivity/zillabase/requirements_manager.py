import importlib.metadata
import subprocess
import sys
import os

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
            print(f"Package '{package_name}' not found. Installing...")
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
