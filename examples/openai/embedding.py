# imports
import ast  # for converting embeddings saved as strings back to arrays
from openai import OpenAI # for calling the OpenAI API
import pandas as pd  # for storing text and embeddings data
import tiktoken  # for counting tokens
import os # for getting API token from env variable OPENAI_API_KEY
from scipy import spatial  # for calculating vector similarities for search
from dotenv import load_dotenv
import random
num = random.random()

# Load environment variables from the .env file (if present)
load_dotenv()

# models
EMBEDDING_MODEL = "text-embedding-ada-002"
GPT_MODEL = "gpt-4o-mini"

client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY", "<your OpenAI API key if not set as env var>"))

def gen_data():
    users = ["Allen","Bertollo","Candice","Dennis","Elaine","Fred","Greg"]
    random.shuffle(users)
    return {
        "sender": f'{users.pop()}',
        "receiver": f'{users.pop()}',
        "amount": random.randrange(100, 5000, 100),
        "risk": random.choice(["safe","risky"]),
    }

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

input_file = "transactions_1k.csv"
embeddings_path = f'embedded_{input_file}'

# Creating dummy transactions and embeddings
# df = pd.DataFrame({
#     'sender': [],
#     'receiver': [],
#     'amount': [],
#     'risk': [],
#     'summary': [],
#     })
# for _ in range(50):
#     row = gen_data()
#     df = df._append({
#     'sender': row.get("sender"),
#     'receiver': row.get("receiver"),
#     'amount': row.get("amount"),
#     'risk': row.get("risk"),
#     'summary': f'{row.get("sender")} transferring ${row.get("amount")} to {row.get("receiver")} is {row.get("risk")}',
#     }, ignore_index = True)

# df.to_csv(input_file)

# df['ada_embedding'] = df.summary.apply(lambda x: get_embedding(x))
# df.to_csv(embeddings_path, index=False)

embeddings_path = f'embedded_{input_file}'
df = pd.read_csv(embeddings_path)
# convert embeddings from CSV str type back to list type
df['ada_embedding'] = df['ada_embedding'].apply(ast.literal_eval)

# examples
row = gen_data()
new_trans = f'{row.get("sender")} transfer ${row.get("amount")} to {row.get("receiver")}'
print(f'Incoming transaction: {new_trans}')
strings, relatednesses = strings_ranked_by_relatedness(new_trans, df, top_n=5)
transaction_history = []
for string, relatedness in zip(strings, relatednesses):
    transaction_history.append(string)

collection = "\n".join(transaction_history)
query = f"""Use the below collection of safe and risky money transfer requests to answer the subsequent question along with your own opinion.

Collection:
\"\"\"
{collection}
\"\"\"

Question: Should {new_trans}?"""
print(query)
response = client.chat.completions.create(
    messages=[
        {'role': 'system', 'content': 'You recommend the amount of fraud risk for money transfers between two people and respond only with a JSON object containing your summary and a numeric value of the risk where 0 is no risk and 100 is the most risk.'},
        {'role': 'user', 'content': query},
    ],
    model=GPT_MODEL,
    temperature=0,
)

print(response.choices[0].message.content)
