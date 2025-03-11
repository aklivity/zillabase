import os
import threading

from requirements_manager import get_requirements_packages, ensure_packages_installed
from class_discovery import fetch_class_files, discover_functions
from flask_routes import app
from udf_server import create_udf_server

def start_flask_server():

    app.run(host="0.0.0.0", port=5000)

def main():
    try:
        requirements_file_path = "/opt/udf/lib/requirements.txt"
        requirements_packages = get_requirements_packages(requirements_file_path)
        ensure_packages_installed(requirements_packages)

        class_files = fetch_class_files()
        discovered_functions = discover_functions(class_files)

        from flask_routes import functions
        functions[:] = discovered_functions

        flask_thread = threading.Thread(target=start_flask_server, daemon=True)
        flask_thread.start()

        server = create_udf_server(discovered_functions, host="0.0.0.0", port=8816)
        server.serve()

    except Exception as ex:
        print(f"An error occurred: {ex}")

if __name__ == "__main__":
    main()
