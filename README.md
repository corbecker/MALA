# MALA
An android application for Adult Literacy e-learning (Optical character recognition, E-learning, Text-To-Speech, Speech-Recognition)

MALA is a Mobile Adult Literacy Application designed to assist adults with literacy issues.
The application does this by providing learning opportunities through technologies 
like Text To Speech, Speech Recognition, and Optical Character Recognition. 
By providing a series of learning exercises within the application and a suite of tools to
assist literacy learners the application supports the theory of experiential learning. 

The application is developed in Java and XML for layout. It uses Android built in libararies for the 
Text To Speech and Speech Recognition functionalities and the Tesseract Open Source Optical Character 
Recognition library for OCR. 

The application provides the user with activities like fill in the sentence and a set of tools to take a picture of a word, say a word,
or manually input a word to hear the word spoken back to them, a description of the word and a picture of the subject at hand.

Particular attention was paid to the User Interface and User Experience design to keep it as simple and intuitive 
as possible to provide a friendly learning environment that supports exploration and successful learning. 

The application requires an android device with an 8mp camera or greater for the OCR functionalities, if the application
detects no camera these features are excluded. 

To install on Android device: 

1. Download libraries file here: https://www.dropbox.com/s/7rapz8om7qk3gzb/libraries.zip?dl=0
2. Unzip and move libraries file to AdultLiteracyApp directory.
3. Open Android Studio.
4. File>Import> Project> AdultLiteracyApp || Open Existing Android Studio App> AdultLiteracyApp
5. Click 'Run' to run on emulator or connected device (Emulator will not support TTS, SR or OCR functionality)

