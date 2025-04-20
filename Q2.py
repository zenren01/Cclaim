from flask import Flask, render_template, request, jsonify
import numpy as np
import cv2
from datasketch import MinHashLSH, MinHash

app = Flask(__name__)

# Home route
@app.route('/')


def home():

    return render_template('index1.html')

# Route to handle image upload and processing

@app.route('/upload', methods=['POST'])

def upload():

    if request.method == 'POST':
        # Get the uploaded images

        img1 = request.files['img1']

        img2 = request.files['img2']


        # Read images
        img1_data = img1.read()

        img2_data = img2.read()

        # Preprocess images
        img1_processed = preprocess_image(img1_data)

        img2_processed = preprocess_image(img2_data)

        # Convert images to vectors
        vector1 = convert_to_vector(img1_processed)

        vector2 = convert_to_vector(img2_processed)

        # Calculate Jaccard similarity score
        similarity_score = calculate_similarity(vector1, vector2)


        # Return similarity score as JSON

        return jsonify({'similarity': similarity_score})

# Preprocess image function

def preprocess_image(image_data):
    # Convert image data to NumPy array

    img_np = np.frombuffer(image_data, np.uint8)
    # Decode image using OpenCV

    img = cv2.imdecode(img_np, cv2.IMREAD_COLOR)
    
    

    # Resize image to a fixed size (e.g., 256x256)
    img = cv2.resize(img, (256, 256))


    #Convert image to grayscale

    img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    #  Apply Gaussian blur

    img = cv2.GaussianBlur(img, (5, 5), 0)
    
    # Return preprocessed image

    return img

# Convert image to vector function

def convert_to_vector(image):
    # Convert image to vector form

    return image.flatten()

def calculate_similarity(vector1, vector2):

    # Create MinHash objects for both vectors
    m1 = MinHash()

    m2 = MinHash()
    for val in vector1:
        m1.update(str(val).encode('utf8'))

    for val in vector2:
        m2.update(str(val).encode('utf8'))


    # Calculate Jaccard similarity score
    jaccard_similarity = m1.jaccard(m2)


    return jaccard_similarity

if __name__ == '__main__':

    app.run(debug=True)
