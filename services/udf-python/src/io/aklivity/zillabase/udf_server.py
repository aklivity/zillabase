from arrow_udf import UdfServer

def create_udf_server(functions, host="0.0.0.0", port=8816):
    server = UdfServer(location=f"{host}:{port}")
    for function in functions:
        server.add_function(function)
    return server
