# imports
import ast  # for converting embeddings saved as strings back to arrays
from openai import OpenAI # for calling the OpenAI API
import pandas as pd  # for storing text and embeddings data
import tiktoken  # for counting tokens
import os # for getting API token from env variable OPENAI_API_KEY
from scipy import spatial  # for calculating vector similarities for search
from dotenv import load_dotenv

# Load environment variables from the .env file (if present)
load_dotenv()

# models
EMBEDDING_MODEL = "text-embedding-ada-002"
GPT_MODEL = "gpt-4o-mini"

client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY", "<your OpenAI API key if not set as env var>"))

# text copied and pasted from: https://en.wikipedia.org/wiki/Swimming_at_the_2024_Summer_Olympics
# I didn't bother to format or clean the text, but GPT will still understand it
# the entire article is too long for gpt-3.5-turbo, so I only included the top few sections

transaction_history = """
[
userA tranfering $200 to userB is safe,
userA transferring $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userA tranfering $200 to userB is safe,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky,
userB transfering $1000 to userA is risky
]
"""

# query = f"""Use the below collection of safe and risky money transfer requests to answer the subsequent question along with your own opinion. If the answer cannot be found, write "I can't determine fraud risk"

# Collection:
# \"\"\"
# {transaction_history}
# \"\"\"

# Question: How risky is it for userB to transfer $1000 to userA?"""

# response = client.chat.completions.create(
#     messages=[
#         {'role': 'system', 'content': 'You recommend the fraud risk for money transfers between users. You respond with a number where 0 is no risk and 100 is the most risk.'},
#         {'role': 'user', 'content': query},
#     ],
#     model=GPT_MODEL,
#     temperature=0,
# )

# print(response.choices[0].message.content)


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
        (row["text"], relatedness_fn(query_embedding, row["embedding"]))
        for i, row in df.iterrows()
    ]
    strings_and_relatednesses.sort(key=lambda x: x[1], reverse=True)
    strings, relatednesses = zip(*strings_and_relatednesses)
    return strings[:top_n], relatednesses[:top_n]

