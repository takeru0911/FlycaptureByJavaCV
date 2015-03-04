import static org.bytedeco.javacpp.FlyCapture2.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.bytedeco.javacpp.FlyCapture2.BusManager;
import org.bytedeco.javacpp.FlyCapture2.Camera;
import org.bytedeco.javacpp.FlyCapture2.CameraInfo;
import org.bytedeco.javacpp.FlyCapture2.Error;
import org.bytedeco.javacpp.FlyCapture2.FC2Version;
import org.bytedeco.javacpp.FlyCapture2.Image;
import org.bytedeco.javacpp.FlyCapture2.PGRGuid;
import org.bytedeco.javacpp.FlyCapture2.Utilities;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_highgui;




import org.bytedeco.javacpp.opencv_highgui.CvVideoWriter;
import org.bytedeco.javacpp.presets.FlyCapture2;



public class FlyCapture {

	static void PrintBuildInfo() {
		FC2Version fc2Version = new FC2Version();
		Utilities.GetLibraryVersion(fc2Version);
		System.out.println("FlyCapture2 library version: "
				+ fc2Version.major() + "." + fc2Version.minor() + "."
				+ fc2Version.type() + "." + fc2Version.build());

		System.out.println("JavaCPP Presets version: "
				+ FlyCapture2.class.getPackage().getImplementationVersion());
	}

	static void PrintCameraInfo(CameraInfo pCamInfo) {
		System.out.println(
				"\n*** CAMERA INFORMATION ***\n"
						+ "Serial number - " + pCamInfo.serialNumber() + "\n"
						+ "Camera model - " + pCamInfo.modelName().getString() + "\n"
						+ "Camera vendor - " + pCamInfo.vendorName().getString() + "\n"
						+ "Sensor - " + pCamInfo.sensorInfo().getString() + "\n"
						+ "Resolution - " + pCamInfo.sensorResolution().getString() + "\n"
						+ "Firmware version - " + pCamInfo.firmwareVersion().getString() + "\n"
						+ "Firmware build time - " + pCamInfo.firmwareBuildTime().getString() + "\n");

	}

	static void PrintError(Error error) {
		error.PrintErrorTrace();
	}

	static int RunSingleCamera(PGRGuid guid) {
		final int k_numImages = 10;

		Error error;
		Camera cam = new Camera();

		// Connect to a camera
		error = cam.Connect(guid);
		// cam.SetVideoModeAndFrameRate(VIDEOMODE_, FRAMERATE_30);

		if (error.notEquals(PGRERROR_OK)) {
			PrintError(error);
			return -1;
		}

		// Get the camera information
		CameraInfo camInfo = new CameraInfo();
		error = cam.GetCameraInfo(camInfo);
		if (error.notEquals(PGRERROR_OK)) {
			PrintError(error);
			return -1;
		}

		PrintCameraInfo(camInfo);

		// Start capturing images
		error = cam.StartCapture();

		if (error.notEquals(PGRERROR_OK)) {
			PrintError(error);
			return -1;
		}

		Image rawImage = new Image();
		//opencv_highgui.CV_FOURCC((byte)'D', (byte)'I', (byte)'B', (byte)' ')
		CvVideoWriter vw = opencv_highgui.cvCreateVideoWriter("temp.avi", opencv_highgui.CV_FOURCC((byte)'D', (byte)'I', (byte)'B', (byte)' '),30.0 , opencv_core.cvSize(640, 480), 1);
		for (int imageCnt = 0; imageCnt < 300; imageCnt++) {
			// Retrieve an image
			error = cam.RetrieveBuffer(rawImage);

			if (error.notEquals(PGRERROR_OK)) {
				PrintError(error);
				continue;
			}
			System.out.println(rawImage.GetPixelFormat());
			System.out.println("Grabbed image " + imageCnt);

			// Create a converted image
			Image convertedImage = new Image();

			// Convert the raw image
			error = rawImage.Convert(PIXEL_FORMAT_BGR, convertedImage);


			Mat mat = new Mat(convertedImage.GetRows(), convertedImage.GetCols(), org.bytedeco.javacpp.opencv_core.CV_8UC3, convertedImage.GetData());
			//
			IplImage img = mat.asIplImage();

			if (error.notEquals(PGRERROR_OK)) {
				PrintError(error);
				return -1;
			}
			System.out.println(mat);
			opencv_highgui.cvShowImage("takeru", img);
			opencv_highgui.cvWriteFrame (vw, img);
			opencv_highgui.cvWaitKey(1);
			// Create a unique filename
			//String filename = camInfo.serialNumber() + "-" + imageCnt + ".png";

			// Save the image. If a file format is not passed in, then the file
			// extension is parsed to attempt to determine the file format.
			// error = convertedImage.Save(filename);
			if (error.notEquals(PGRERROR_OK)) {
				PrintError(error);
				return -1;
			}


		}
		opencv_highgui.cvReleaseVideoWriter(vw);
		// Stop capturing images
		error = cam.StopCapture();
		if (error.notEquals(PGRERROR_OK)) {
			PrintError(error);
			return -1;
		}

		// Disconnect the camera
		error = cam.Disconnect();
		if (error.notEquals(PGRERROR_OK)) {
			PrintError(error);
			return -1;
		}

		return 0;
	}

	public static void main(String[] args) throws IOException {
		PrintBuildInfo();

		Error error;

		// Since this application saves images in the current folder
		// we must ensure that we have permission to write to this folder.
		// If we do not have permission, fail right away.
		File tempFile = new File("test.txt");
		try {
			new FileOutputStream(tempFile).close();
		} catch (IOException e) {
			System.out.println("Failed to create file in current folder.  "
					+ "Please check permissions.");
			System.exit(-1);
		}
		tempFile.delete();

		BusManager busMgr = new BusManager();
		int[] numCameras = new int[1];
		error = busMgr.GetNumOfCameras(numCameras);
		if (error.notEquals(PGRERROR_OK)) {
			PrintError(error);
			System.exit(-1);
		}

		System.out.println("Number of cameras detected: " + numCameras[0]);

		for (int i = 0; i < numCameras[0]; i++) {
			PGRGuid guid = new PGRGuid();
			error = busMgr.GetCameraFromIndex(i, guid);
			if (error.notEquals(PGRERROR_OK)) {
				PrintError(error);
				System.exit(-1);
			}

			RunSingleCamera(guid);
		}

		//IplImage img = opencv_highgui.cvvLoadImage("0.png");
		//  CvMat mat = img.asCvMat();
		//  mat.memset(convertedImage, 2, 2);
		//System.out.println(mat.width());
		// System.out.println(convertedImage.asByteBuffer().toString());

	//	opencv_highgui.cvShowImage("takeru", img);

	}
}
