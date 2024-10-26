import cv2
import numpy as np
import socket
import google.generativeai as genai
import struct
import os
from HairLipsColorChanger import apply_makeup
from image import apply_eyeshadow_and_blush
import sys

# Configure the Google Generative AI
genai.configure(api_key="AIzaSyDaOwMml2mIpNGqRcTN7xaeeWrjpj2iFGo")

generation_config = {
    "temperature": 0.9,
    "top_p": 1,
    "top_k": 0,
    "max_output_tokens": 2048,
    "response_mime_type": "text/plain",
}

safety_settings = [
    {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
    {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
]

model = genai.GenerativeModel(
    model_name="gemini-1.0-pro",
    safety_settings=safety_settings,
    generation_config=generation_config,
)

chat_session = model.start_chat()

# Load the pre-trained face detector
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

def detect_face(frame):
    frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    
    # Convert to grayscale for face detection
    gray = cv2.cvtColor(frame_rgb, cv2.COLOR_RGB2GRAY)
    faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))
    
    if len(faces) > 0:
        x, y, w, h = faces[0]
        padding = 20  # Add padding around the detected face
        x -= padding
        y -= padding
        w += 2 * padding
        h += 2 * padding
        face_frame = frame_rgb[max(0, y):min(frame.shape[0], y+h), max(0, x):min(frame.shape[1], x+w)]
        avg_rgb = np.mean(face_frame, axis=(0, 1))
        print(f"Average RGB Color of Face: {avg_rgb}")
        return face_frame, avg_rgb
    else:
        print("No face detected!")
        return None, None

def check_lighting(frame):
    brightness = np.mean(cv2.cvtColor(frame, cv2.COLOR_RGB2GRAY))
    if brightness < 50:
        print("Warning: Lighting is too dim!")
    elif brightness > 200:
        print("Warning: Lighting is too bright!")

def analyze_face(frame):
    face_frame, avg_rgb = detect_face(frame)
    if face_frame is not None and avg_rgb is not None:
        check_lighting(face_frame)
        prompt_values = str(avg_rgb)
        pre_prompt = '''Determine the Seasonal Color Analysis (category: Winter, Spring, Summer, or Autumn) based on the RGB values of the face. Ensure accuracy that the season is correct.
        
         If the RGB values from previous attempts are the same, the best colors that will be provided should be identical. However, if the RGB values is different, even within the same season, the best colors should be different(the colors should be within the range of the season, just give different set of colors.) 

            Important note: The best colors should be distinct and unique for each different average RGB color. No two different RGB colors should share the same set of best colors. exactly the same RGB color should have the exactly the same best colors.
        
            make sure it follows this template
            Seasonal Color Analysis(Winter, Spring, Summer or Autumn): -----------
            Best Colors Palette (6 best colors. Should be in hex.): -----------
            
            but in this test, make sure that autumn is the result
            '''
        response = chat_session.send_message(pre_prompt + prompt_values)
        # Print the response in the terminal (for debugging purposes)
        print(response.text)
        return response.text
    return "No face detected or lighting issue."

def receive_all(socket, length):
    """Receive length bytes from the socket."""
    data = b''
    while len(data) < length:
        remaining_bytes = length - len(data)
        data += socket.recv(4096 if remaining_bytes > 4096 else remaining_bytes)
    return data

def display_image(image_path):
    """Display the image using OpenCV"""
    image = cv2.imread(image_path)
    cv2.imshow('Processed Image', image)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

def server_program():
    host = '0.0.0.0'  # Listen on all available interfaces
    port = 5000  # Port to listen on

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, port))
    server_socket.listen(1)
    print("Server listening on port", port)

    while True:
        conn, address = server_socket.accept()
        print("Connection from:", address)

        result_image_path = None  # Initialize variable to avoid UnboundLocalError

        try:
            # Receive the identifier length and identifier
            identifier_length_data = receive_all(conn, 4)
            if len(identifier_length_data) < 4:
                print("Error: Incomplete identifier length data received")
                conn.close()
                continue
            identifier_length = struct.unpack('>I', identifier_length_data)[0]
            identifier = receive_all(conn, identifier_length).decode()

            print(f"Received data from: {identifier}")

            # Check if the identifier matches "Analysis" or "Changer"
            if identifier == "Analysis":
                # Receive the length of the image data (4 bytes)
                length_data = receive_all(conn, 4)
                if len(length_data) < 4:
                    print("Error: Incomplete length data received")
                    conn.close()
                    continue

                # Unpack the length data
                length = struct.unpack('>I', length_data)[0]

                # Receive the image data
                frame_data = receive_all(conn, length)

                # Convert the byte data to a NumPy array
                frame_array = np.frombuffer(frame_data, dtype=np.uint8)
                frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)
                
                print("Image received for Analysis.")
                result = analyze_face(frame)
                
                conn.send(result.encode())
            
            elif identifier == "Changer":
                # Receive the length of the image data (4 bytes)
                length_data = receive_all(conn, 4)
                if len(length_data) < 4:
                    print("Error: Incomplete length data received")
                    conn.close()
                    continue

                # Unpack the length data
                length = struct.unpack('>I', length_data)[0]

                # Receive the image data
                frame_data = receive_all(conn, length)

                # Convert the byte data to a NumPy array
                frame_array = np.frombuffer(frame_data, dtype=np.uint8)
                frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)
                original_size = frame.shape[:2]  # Store the original size (height, width)
                temp_image_path = 'temp_image.jpg'
                cv2.imwrite(temp_image_path, frame)
                print("Image received for Changer.")

                # Receive hair, lip, blush color and eyeshadow color values
                hair_color_length_data = receive_all(conn, 4)
                hair_color_length = struct.unpack('>I', hair_color_length_data)[0]
                hair_color_data = receive_all(conn, hair_color_length)
                hair_color = [int(hair_color_data[i]) for i in range(3)]

                lip_color_length_data = receive_all(conn, 4)
                lip_color_length = struct.unpack('>I', lip_color_length_data)[0]
                lip_color_data = receive_all(conn, lip_color_length)
                lip_color = [int(lip_color_data[i]) for i in range(3)]

                blush_color_length_data = receive_all(conn, 4)
                blush_color_length = struct.unpack('>I', blush_color_length_data)[0]
                blush_color_data = receive_all(conn, blush_color_length)
                blush_color = [int(blush_color_data[i]) for i in range(3)]

                eyeshadow_color_length_data = receive_all(conn, 4)
                eyeshadow_color_length = struct.unpack('>I', eyeshadow_color_length_data)[0]
                eyeshadow_color_data = receive_all(conn, eyeshadow_color_length)
                eyeshadow_color = [int(eyeshadow_color_data[i]) for i in range(3)]

                print(f"Received hair color (RGB): {hair_color}")
                print(f"Received lip color (RGB): {lip_color}")
                print(f"Received blush color (RGB): {blush_color}")
                print(f"Received eyeshadow color (RGB): {eyeshadow_color}")

                # Call the HairLipsColorChanger function
                result_image_firstpart = apply_makeup(temp_image_path, hair_color, lip_color)
                print("Hair and Lip Applied")

                # Call the function
                result_image_path = apply_eyeshadow_and_blush(result_image_firstpart, blush_color, eyeshadow_color)

                print("Blush and Eyeshadow Applied")


                if result_image_path is not None:
                    result_image = cv2.imread(result_image_path)
                    result_image_resized = cv2.resize(result_image, (original_size[1], original_size[0]))  # Resize back to original size
                    result_image_path_resized = 'output_resized.png'
                    cv2.imwrite(result_image_path_resized, result_image_resized)

                    with open(result_image_path_resized, 'rb') as f:
                        result_image_data = f.read()

                    # Send the length of the result image data and the result image data
                    conn.send(struct.pack('>I', len(result_image_data)))
                    conn.send(result_image_data)
                    print("Processed image sent back to client.")

                    # Clean up the resized result image
                    os.remove(result_image_path_resized)
                else:
                    conn.send(b"Error applying makeup")

                # Clean up the temporary image
                os.remove(temp_image_path)
                if result_image_path:
                    os.remove(result_image_path)
            
            else:
                print("Invalid identifier, closing connection.")
                conn.close()
                continue

        except Exception as e:
            print(f"Error: {str(e)}")

        finally:
            conn.close()

if __name__ == "__main__":
    server_program()