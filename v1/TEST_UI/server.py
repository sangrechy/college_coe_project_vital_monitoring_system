from flask import Flask, request, jsonify, render_template
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

latest_data = {
    "hr": 0,
    "spo2": 0,
    "ir": 0
}

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/data', methods=['POST'])
def receive_data():

    global latest_data

    latest_data = request.json

    print(latest_data)

    return jsonify({"status": "success"})

@app.route('/get_data')
def get_data():
    return jsonify(latest_data)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)

