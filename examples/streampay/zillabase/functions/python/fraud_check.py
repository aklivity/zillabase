import time
from arrow_udf import udf, udtf

@udf(input_types=['VARCHAR', 'VARCHAR', 'DOUBLE PRECISION'], result_type='STRUCT<summary: VARCHAR, risk: INT>')
def assess_fraud(from_username, to_username, amount):
    print(amount)
    return {
        "summary": "Based on the previous data, Allen transferring money to Bertollo has been identified as risky "
                   "in one instance. Although Allen has made safe transfers to others, the specific transfer to "
                   "Bertollo raises concerns. Therefore, it is advisable to consider the risk before proceeding.",
        "risk": 70
    }

@udf(input_types=['VARCHAR', 'VARCHAR', 'DOUBLE PRECISION', 'VARCHAR'], result_type='BOOLEAN')
def process_embedding(from_username, to_username, amount, event):
    print(event)
    return True
