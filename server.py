from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

app = Flask(__name__)

model_name = "facebook/nllb-200-1.3B"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

@app.route("/translate", methods=["POST"])
def translate():
    data = request.json
    text = data.get("text", "")
    if not text:
        return jsonify({"error": "No text provided"}), 400

    src_lang = "eng_Latn"
    tgt_lang = "kor_Hang"
    tokenizer.src_lang = src_lang
    inputs = tokenizer(text, return_tensors="pt")
    forced_bos_token_id = tokenizer.convert_tokens_to_ids(tgt_lang)
    generated = model.generate(**inputs, forced_bos_token_id=forced_bos_token_id)
    result = tokenizer.decode(generated[0], skip_special_tokens=True)

    return jsonify({"translation": result})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)