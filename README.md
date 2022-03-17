# OpenCVScanner 
### Project Dated :2019
#### This project is just for the sake of demonstration purpose of how to make use of CameraX and OpenCV together along with satisfying the MVVM architecture. This project is no longer maintained and is an old project.


This project is built for the purpose of demonstrating the power of OpenCV and Android together.The Project has a Camera Fragment which takes help of the CameraX library to take the picture of the object to be scanned and passes it for the purpose of cropping to Scan Fragment and on clicking on the done button, PDF is created.All the pdf can be viewed at the Home Fragment and can also individual PDF's can be viewed in detail by clicking on it.The application also has a Gallery Fragment for picking images from the gallery.<br>
The main aim of this project was also to calibrate the contour detection algorithm, so that images are scanned hassle-free. Different algorithms with different paramters were tried to bring efficiency to the existing algorithm.This project served as an experimentation bit for the purposing of optimizing the contour detection, so might not be in the state that the project is trying to depict but can be brought back to normal by connecting the moved pieces.

The libraries used are : <br>
OpenCV - Used for the pupose of crafting algorithm that are used for the purpose of scanning the image which includes getting the contours and applying filters.<br>
Room - For storing information about the PDF's and the images captured <br>
Navigation Component - For the purpose of navigating between the fragment <br>
LiveData - For providing data from the ViewModel to the Fragments<br>
ViewModel - For enhancing the flow and taking care of orientation configurations <br>
CameraX - For capturing images and harnessing the power of the stability it provides for making sure that most devices are supported <br>
Glide - For displaying Images <br>


For using the project, one need to integrate OpenCV , one can do it the hard-way by following tutorials provided here https://opencv.org/android/ or make use of QuickBirdStudio library https://github.com/quickbirdstudios/opencv-android. One also needs to manage the gradle to bring the gradle configurations as per their AGP.
