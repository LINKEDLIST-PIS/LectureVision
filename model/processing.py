import cv2

def apply_mosaic(frame, boxes, grid=12):
    for (x1, y1, x2, y2) in boxes:
        roi = frame[y1:y2, x1:x2]
        if roi.size == 0:
            continue
        roi_small = cv2.resize(roi, (max(1, (x2-x1)//grid), max(1, (y2-y1)//grid)))
        roi_mosaic = cv2.resize(roi_small, (x2-x1, y2-y1), interpolation=cv2.INTER_NEAREST)
        frame[y1:y2, x1:x2] = roi_mosaic
    return frame