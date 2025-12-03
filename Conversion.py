import os
import json
import cv2
from tqdm import tqdm

def load_odgt(odgt_path):
    records = []
    with open(odgt_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            records.append(json.loads(line))
    return records

def clip_box(x1, y1, w, h, img_w, img_h):
    x2 = x1 + w
    y2 = y1 + h
    x1 = max(0, min(x1, img_w - 1))
    y1 = max(0, min(y1, img_h - 1))
    x2 = max(0, min(x2, img_w - 1))
    y2 = max(0, min(y2, img_h - 1))
    w = max(0, x2 - x1)
    h = max(0, y2 - y1)
    return x1, y1, w, h

def convert_record_to_yolo_lines(record, img_w, img_h):
    lines = []
    for ann in record.get('gtboxes', []):
        if ann.get('tag') != 'person':
            continue
        extra = ann.get('extra', {})
        if extra.get('ignore', 0) == 1:
            continue
        fbox = ann.get('fbox')
        if not fbox or len(fbox) != 4:
            continue
        x1, y1, w, h = fbox

        if w <= 1 or h <= 1:
            continue

        x1, y1, w, h = clip_box(x1, y1, w, h, img_w, img_h)
        if w <= 1 or h <= 1:
            continue

        x_center = (x1 + w / 2.0) / img_w
        y_center = (y1 + h / 2.0) / img_h
        w_norm = w / img_w
        h_norm = h / img_h

        lines.append(f"0 {x_center:.6f} {y_center:.6f} {w_norm:.6f} {h_norm:.6f}")
    return lines

def convert_odgt_to_yolo(odgt_path, images_dir, labels_dir, image_exts=('.jpg', '.png', '.jpeg')):
    os.makedirs(labels_dir, exist_ok=True)
    records = load_odgt(odgt_path)

    missing_images = []
    converted = 0

    for rec in tqdm(records, desc=f"Converting {os.path.basename(odgt_path)}"):
        img_id = rec.get('ID')
        if not img_id:
            img_id = rec.get('filename') or rec.get('image_id')
        if not img_id:
            continue

        img_path = None
        for ext in image_exts:
            candidate = os.path.join(images_dir, f"{img_id}{ext}") if '.' not in img_id else os.path.join(images_dir, img_id)
            if os.path.exists(candidate):
                img_path = candidate
                break

        if img_path is None:
            p = rec.get('path')
            if p and os.path.exists(os.path.join(images_dir, p)):
                img_path = os.path.join(images_dir, p)

        if img_path is None or not os.path.exists(img_path):
            missing_images.append(img_id)
            continue

        img = cv2.imread(img_path)
        if img is None:
            missing_images.append(img_id)
            continue
        img_h, img_w = img.shape[:2]

        lines = convert_record_to_yolo_lines(rec, img_w, img_h)
        label_name = os.path.splitext(os.path.basename(img_path))[0] + ".txt"
        label_path = os.path.join(labels_dir, label_name)

        if lines:
            with open(label_path, 'w', encoding='utf-8') as lf:
                lf.write("\n".join(lines))
            converted += 1
        else:
            pass

    print(f"Converted labels: {converted}")
    if missing_images:
        print(f"Missing images for {len(missing_images)} records (check ID/exts). Example IDs: {missing_images[:5]}")

if __name__ == "__main__":
    convert_odgt_to_yolo(
        odgt_path="datasets/crowdhuman/annotation_train.odgt",
        images_dir="datasets/crowdhuman/train/images",
        labels_dir="datasets/crowdhuman/train/labels"
    )

    convert_odgt_to_yolo(
        odgt_path="datasets/crowdhuman/annotation_val.odgt",
        images_dir="datasets/crowdhuman/val/images",
        labels_dir="datasets/crowdhuman/val/labels"
    )