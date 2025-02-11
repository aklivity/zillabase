from flask import Flask, jsonify

app = Flask(__name__)

functions = []

@app.route("/python/methods", methods=["GET"])
def list_methods():
    global functions
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
