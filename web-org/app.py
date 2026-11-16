import os

from flask import Flask, render_template

app = Flask(__name__)

# en Athenet pasa por un BFF; la maqueta lo omite y apunta a ms-competitions
URL_COMPETITIONS = os.getenv("URL_COMPETITIONS", "http://localhost:8081")


@app.get("/")
def index():
    return render_template("index.html", url_competitions=URL_COMPETITIONS)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
