import os
import re
import inspect
import importlib.util
from pathlib import Path

CLASS_NAME_PATTERN = re.compile(r"([a-z])([A-Z])")

def fetch_class_files():
    class_files = []
    classpath = os.environ.get("PYTHONPATH", None)
    if classpath:
        paths = classpath.split(os.pathsep)
        for path in paths:
            if path.startswith("/opt/udf/lib"):
                path_obj = Path(path)
                if path_obj.is_dir():
                    for file in path_obj.rglob("*.py"):
                        class_files.append(file)
    return class_files

def dynamic_import(file_path):
    module_name = file_path.stem
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def discover_functions(class_files):
    functions = []
    for class_file in class_files:
        try:
            module = dynamic_import(class_file)
            print(f"Module loaded: {class_file}")
            for name, function in inspect.getmembers(module):
                if (
                    hasattr(function, '__module__') and
                    function.__module__ == 'arrow_udf' and
                    hasattr(function, '_name')
                ):
                    functions.append(function)
        except (ModuleNotFoundError, AttributeError, ImportError) as ex:
            print(f"Error loading {class_file}: {ex}")
    return functions
