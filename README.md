# locationtracking

 ![ScreenShot](https://github.com/manishkummar21/locationtracking/blob/master/device-2019-02-13-170117.png)



Step1 : Add the Support Map Fragment and with location request configuration.

Step2 : Set the setSmallestDisplacement to 100 meter its mean onLocationResult callback will be triggered only when the location for 100 meter. Why I doing instead of time because it reduce number of callbacks.

