import json
from arrow_udf import udf # for registering User Defined Functions
import ast  # for converting embeddings saved as strings back to arrays
from openai import OpenAI # for calling the OpenAI API
import pandas as pd  # for storing text and embeddings data
import os # for getting API token from env variable OPENAI_API_KEY
from scipy import spatial  # for calculating vector similarities for search
from dotenv import load_dotenv

# Get example transaction history data and load into a pandas DataFrame
from transaction_data import transaction_data_csv
history_df = pd.read_csv(transaction_data_csv)
history_df['ada_embedding'] = history_df['ada_embedding'].apply(ast.literal_eval)

# Load environment variables from the .env file (if present)
load_dotenv()

# OpenAI models
EMBEDDING_MODEL = "text-embedding-ada-002"
GPT_MODEL = "gpt-4o-mini"
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY", "<your OpenAI API key if not set as env var>"))

# generate embedings
def get_embedding(text):
   text = text.replace("\n", " ")
   return client.embeddings.create(input = [text], model=EMBEDDING_MODEL).data[0].embedding

# search function
def strings_ranked_by_relatedness(
    query: str,
    df: pd.DataFrame,
    relatedness_fn=lambda x, y: 1 - spatial.distance.cosine(x, y),
    top_n: int = 100
) -> tuple[list[str], list[float]]:
    """Returns a list of strings and relatednesses, sorted from most related to least."""
    query_embedding_response = client.embeddings.create(
        model=EMBEDDING_MODEL,
        input=query,
    )
    query_embedding = query_embedding_response.data[0].embedding
    strings_and_relatednesses = [
        (row["summary"], relatedness_fn(query_embedding, row["ada_embedding"]))
        for i, row in df.iterrows()
    ]
    strings_and_relatednesses.sort(key=lambda x: x[1], reverse=True)
    strings, relatednesses = zip(*strings_and_relatednesses)
    return strings[:top_n], relatednesses[:top_n]


@udf(input_types=['VARCHAR', 'VARCHAR', 'DOUBLE PRECISION'], result_type='STRUCT<summary: VARCHAR, risk: VARCHAR>')
def assess_fraud(from_username, to_username, amount):
    new_trans = f'{from_username} transfer ${amount} to {to_username}'
    print(f'assess_fraud: {new_trans}')
    strings, relatednesses = strings_ranked_by_relatedness(new_trans, history_df, top_n=10)
    transaction_history = []
    for string, _ in zip(strings, relatednesses):
        transaction_history.append(string)

    collection = "\n".join(transaction_history)
    query = f"""Use the below collection of safe and risky money transfer requests to answer the subsequent question along with your own opinion.

    Collection:
    \"\"\"{collection}\"\"\"

    Question: Should {new_trans}?"""
    response = client.chat.completions.create(
        messages=[
            {'role': 'system', 'content': 'You recommend the amount of fraud risk for money transfers between two people and respond only with a JSON object containing your summary and a numeric value of the risk as LOW, MEDIUM, or HIGH. Only return valid JSON string and no other markup.'},
            {'role': 'user', 'content': query},
        ],
        model=GPT_MODEL,
        temperature=0,
    )

    result = json.loads(response.choices[0].message.content)
    return result

@udf(input_types=['VARCHAR', 'VARCHAR', 'DOUBLE PRECISION', 'VARCHAR'], result_type='BOOLEAN')
def process_embedding(from_username, to_username, amount, event):
  global history_df
  risk = 'safe'
  if (event == 'PaymentRejected'):
    risk = 'risky'
  summary = f'{from_username} transferring ${amount} to {to_username} is {risk}'
  print(f'process_embedding: {summary}')
  # convert embeddings from CSV str type back to list type
  new_row = {
    'sender': from_username,
    'receiver': to_username,
    'amount': amount,
    'risk': event,
    'summary': summary
  }
  df_new_rows = pd.DataFrame(new_row, index=[0])
  df_new_rows['ada_embedding'] = df_new_rows.summary.apply(lambda x: get_embedding(x))
  history_df = pd.concat([history_df, df_new_rows], ignore_index=True)
  print(history_df)
  return True
