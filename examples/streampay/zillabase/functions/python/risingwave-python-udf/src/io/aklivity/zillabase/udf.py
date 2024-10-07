import time
from arrow_udf import udf, udtf

@udf(input_types=["INT"], result_type="INT", io_threads=32)
def blocking(x):
    time.sleep(0.01)
    return x
