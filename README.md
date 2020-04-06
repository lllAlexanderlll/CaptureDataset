# CaptureDataset
This app implements the environment capturing process with Loomos HD camera (Segway Robotics Loomo) in the context of the term paper "Visual Place Recognition to Support Indoor Localisation"

## Instructions
Place the robot within a place and enter the place label, the x coordinate value of the local coordinate system (X), the y coordinate value of the local coordinate system (Y) and optionally a yaw angle value to calculate the global view direction (yaw).
The given values determine the image filename (date_time_placeLabel_X_Y_yaw_pitch.jpg). Date, time and pitch are set automatically.

**For images taken upside down an EXIF orientation is set. Before training calculating codebook/PCA/index rotate the images accordingly**. On Linux you may use ```exiftran -ai $(find . -type f)``` within the image folder.
