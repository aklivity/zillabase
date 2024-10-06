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

# # Creating dummy transactions and embeddings
# df = pd.DataFrame({
#     'sender': [],
#     'receiver': [],
#     'amount': [],
#     'risk': [],
#     'summary': [],
#     })
# for _ in range(10):
#     users = ["A","B","C","D","E","F","G"]
#     random.shuffle(users)
#     sender = f'user{users.pop()}'
#     receiver = f'user{users.pop()}'
#     amount = random.randrange(100, 5000, 100)
#     risk = random.choice(["safe","risky"])
#     summary = f'{sender} transferring ${amount} to {receiver} is {risk}'
#     df = df._append({
#     'sender': sender,
#     'receiver': receiver,
#     'amount': amount,
#     'risk': risk,
#     'summary': summary,
#     }, ignore_index = True)

# input_datapath = "transactions_1k.csv"
# df.to_csv(input_datapath)

# def get_embedding(text, model="text-embedding-3-small"):
#    text = text.replace("\n", " ")
#    return client.embeddings.create(input = [text], model=model).data[0].embedding

# df['ada_embedding'] = df.summary.apply(lambda x: get_embedding(x, model='text-embedding-3-small'))
# df.to_csv(f'embedded_{input_datapath}', index=False)




