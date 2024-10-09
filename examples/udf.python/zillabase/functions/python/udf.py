import pandas as pd
import time
from arrow_udf import udf, udtf

@udf(input_types=['INT', 'INT'], result_type='INT')
def gcd(x, y):
    while y != 0:
        (x, y) = (y, x % y)
    return x

@udf(input_types=["INT"], result_type="INT", io_threads=32)
def blocking(x):
    time.sleep(0.01)
    return x

@udf(input_types=['VARCHAR'], result_type='STRUCT<key: VARCHAR, value: VARCHAR>')
def key_value(pair: str):
    key, value = pair.split('=')
    return {'key': key, 'value': value}

@udtf(input_types='INT', result_types='INT')
def series(n):
    for i in range(n):
        yield i

@udf(input_types=['VARCHAR'], result_type='INT')
def sum_csv_column(csv_string: str) -> int:
    data = [int(x) for x in csv_string.split(',')]
    df = pd.DataFrame(data, columns=['values'])
    return df['values'].sum()
