# DrOne
Code for Drone-based Android App to watch over senior citizens as they go for walks

ImageClassifier.java: This code classifies an image as either fallen or not fallen using TF Lite model and assigns each image with a probability

MainActivity.java: The main code calls ImageClassifier.java on only the new images taken by the drone. Based on the output it decides to whether send a message or not. It also displays the image taken by the drone and specifies what action it's going to take


