package com.example.android_project03;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	// Identificateurs des messages reçus du "Handler" de "BluetoothService"
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	// Noms clé reçus du "Handler" de "BluetoothService"
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	// Codes de demande de l'Intent
	private static final int REQUEST_CONNECT_DEVICE = 1;
	protected static final int REQUEST_ENABLE_BT = 2;
	// Nom du périphérique Bluetooth connecté
	private String mConnectedDeviceName = null;
	// Classe de l'adaptateur Bluetooth local
	protected BluetoothAdapter mBluetoothAdapter = null;
	// Classe du service "BluetoothService"
	private BluetoothService mBluetoothService = null;

	// Boutons
	ToggleButton btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8;

	/** Méthode appelée quand l'activité est créée la première fois. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Initialisation et affichage de l'écran principal
		setContentView(R.layout.activity_main);
		// Bouton 1
		btn1 = (ToggleButton) findViewById(R.id.button1);
		btn1.setOnClickListener(Light1Listener);

		// Bouton 2
		btn2 = (ToggleButton) findViewById(R.id.button2);
		btn2.setOnClickListener(Light2Listener);

		// Bouton 3
		btn3 = (ToggleButton) findViewById(R.id.button3);
		btn3.setOnClickListener(Light3Listener);

		// Bouton 4
		btn4 = (ToggleButton) findViewById(R.id.button4);
		btn4.setOnClickListener(Light4Listener);

		// Bouton 5
		btn5 = (ToggleButton) findViewById(R.id.button5);
		btn5.setOnClickListener(Light5Listener);

		// Bouton 6
		btn6 = (ToggleButton) findViewById(R.id.button6);
		btn6.setOnClickListener(Light6Listener);

		// Bouton 7
		btn7 = (ToggleButton) findViewById(R.id.button7);
		btn7.setOnClickListener(Light7Listener);

		// Bouton 8
		btn8 = (ToggleButton) findViewById(R.id.button8);
		btn8.setOnClickListener(Light8Listener);

		// Interdire l'accès aux boutons
		btn1.setEnabled(false);
		btn2.setEnabled(false);
		btn3.setEnabled(false);
		btn4.setEnabled(false);
		btn5.setEnabled(false);
		btn6.setEnabled(false);
		btn7.setEnabled(false);
		btn8.setEnabled(false);

		// Changer la couleur du texte des boutons
		btn1.setTextColor(Color.WHITE);
		btn2.setTextColor(Color.WHITE);
		btn3.setTextColor(Color.WHITE);
		btn4.setTextColor(Color.WHITE);
		btn5.setTextColor(Color.WHITE);
		btn6.setTextColor(Color.WHITE);
		btn7.setTextColor(Color.WHITE);
		btn8.setTextColor(Color.WHITE);

		// Connexion
		// Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Demande d'activation de l'adaptateur via un Intent
		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		// L'activité système répond par un appel de la méthode
		// "onActivityResult"
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}

	/**
	 * Méthode appelée à la fermeture d'une activité. Elle identifie la réponse
	 * pour réaliser le traitement correspondant
	 */

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			// L'activité système de demande d'activation de l'adaptateur
			// Bluetooth a été
			// lancée avec cet identifiant
			if (resultCode == Activity.RESULT_OK) {
				// Le module Bluetooth est maintenant validé : démarrer
				// "BluetoothService"
				// pour gérer la connexion et son maintien
				mBluetoothService = new BluetoothService(this, mHandler); // mHandler
																			// ci-dessous
				// Le Handler gère les messages de "BluetoothService" vers cette
				// activité
			} else {
				// L'utilisateur a choisi de ne pas valider le module Bluetooth
				// (ou erreur)
				Toast.makeText(this, "Error : The bluetooth adapter is down!",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		case REQUEST_CONNECT_DEVICE:
			// Si la phase précédente s'est bien déroulée
			if (resultCode == Activity.RESULT_OK) {
				// Connexion au prériphérique souhaité, dont l'adresse mac est
				// connue
				// Adresse Mac du préiphérique précédent
				// String address = "00:13:12:23:54:51"; - HC-05
				String address = "20:13:02:27:10:48";
				// Instanciation de l'objet "BLuetoothDevice"
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Connexion au périphérique
				mBluetoothService.connect(device);// Démarrer le "ConnectThread"
													// pour établir la connexion
			}
			break;
		}
	}

	/**
	 * Handler utilisé pour obtenir les messages du "BluetoothService"
	 */
	@SuppressLint("HandlerLeak")
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {// // Identifier le message
			case MESSAGE_STATE_CHANGE: // L'état du service a changé
				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTING: // Connexion en cours
					setTitle("Connection ...");
					break;
				case BluetoothService.STATE_CONNECTED: // Connexion établie
					setTitle("Connected to " + mConnectedDeviceName);
					// Autoriser l'accès aux boutons
					btn1.setEnabled(true);
					btn2.setEnabled(true);
					btn3.setEnabled(true);
					btn4.setEnabled(true);
					btn5.setEnabled(true);
					btn6.setEnabled(true);
					btn7.setEnabled(true);
					btn8.setEnabled(true);

					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE: // Pas de connexion
					setTitle("Not connected");
					break;
				}
				break;
			case MESSAGE_DEVICE_NAME: // Réception du message comportant le nom
										// du périphérique
				// Sauvegarde du nom du périphérique Bluetooth
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				// Affichage temporaire de ce nom
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST: // Afficher un message "toast"
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	/**
	 * Method for light 1
	 */
	protected OnClickListener Light1Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn1.isChecked() == true) {
				mBluetoothService.write("a");
				// if the light is on
			} else {
				mBluetoothService.write("b");
			}
		}

	};

	/**
	 * Method for light 2
	 */
	protected OnClickListener Light2Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn2.isChecked() == true) {
				mBluetoothService.write("c");
				// if the light is on
			} else {
				mBluetoothService.write("d");
			}
		}

	};
	/**
	 * Method for light 3
	 */
	protected OnClickListener Light3Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn3.isChecked() == true) {
				mBluetoothService.write("e");
				// if the light is on
			} else {
				mBluetoothService.write("f");
			}
		}

	};
	/**
	 * Method for light 4
	 */
	protected OnClickListener Light4Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn4.isChecked() == true) {
				mBluetoothService.write("g");

				// if the light is on
			} else {
				mBluetoothService.write("h");
			}
		}

	};
	/**
	 * Method for light 5
	 */
	protected OnClickListener Light5Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn5.isChecked() == true) {
				mBluetoothService.write("i");

				// if the light is on
			} else {
				mBluetoothService.write("j");
			}
		}

	};
	/**
	 * Method for light 6
	 */
	protected OnClickListener Light6Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn6.isChecked() == true) {
				mBluetoothService.write("k");

				// if the light is on
			} else {
				mBluetoothService.write("l");
			}
		}

	};
	/**
	 * Method for light 7
	 */
	protected OnClickListener Light7Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn7.isChecked() == true) {
				mBluetoothService.write("m");

				// if the light is on
			} else {
				mBluetoothService.write("n");
			}
		}

	};
	/**
	 * Method for light 8
	 */
	protected OnClickListener Light8Listener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// if the light is off
			if (btn8.isChecked() == true) {
				mBluetoothService.write("o");

				// if the light is on
			} else {
				mBluetoothService.write("p");
			}
		}

	};

	/**
	 * Methode appelée lors de la destruction de l'application
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mBluetoothAdapter = null;
		// // Arrêt des services de "BluetoothService"
		if (mBluetoothService != null)
			mBluetoothService.stop();
	}

	/**
	 * Méthode appelée pour créer un menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		// Appel du fichier main.xml comportant les items du menu
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Méthode lors d'un clic sur un item du menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Si c'est une nouvelle connexion demandée
		case R.id.itemReconn: {
			// On arrête tous les services de BluetoothService
			if (mBluetoothService != null)
				mBluetoothService.stop();
			// On redémarre l'application
			// C'est le seul moyen de réaliser une nouvelle connexion propre
			// Sinon des threads peuvent être encore actifs
			MainActivity.this.recreate();
		}
		// Si c'est une déconnexion demandée
		case R.id.itemDisconn: {
			// On arrête tous les services de BluetoothService
			if (mBluetoothService != null)
				mBluetoothService.stop();
			// On interdit l'accès aux boutons
			btn1.setEnabled(false);
			btn2.setEnabled(false);
			btn3.setEnabled(false);
			btn4.setEnabled(false);
			btn5.setEnabled(false);
			btn6.setEnabled(false);
			btn7.setEnabled(false);
			btn8.setEnabled(false);
		}
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Methode appelée quand l'activité est en pause 
	 * Une autre activité estprésente par dessus
	 * Lors d'un appel par exemple
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Pour ne pas prendre de risque on ferme tous les services
		if (mBluetoothService != null)
			mBluetoothService.stop();
		btn1.setEnabled(false);
		btn2.setEnabled(false);
		btn3.setEnabled(false);
		btn4.setEnabled(false);
		btn5.setEnabled(false);
		btn6.setEnabled(false);
		btn7.setEnabled(false);
		btn8.setEnabled(false);
	}

	/**
	 * Methode appelée quand l'activité est en arrêtée
	 * Elle n'est pas encore détruite et continue de tourner
	 */
	@Override
	protected void onStop() {
		super.onStop();
		// Pour ne pas prendre de risque on ferme tous les services
		if (mBluetoothService != null)
			mBluetoothService.stop();
		btn1.setEnabled(false);
		btn2.setEnabled(false);
		btn3.setEnabled(false);
		btn4.setEnabled(false);
		btn5.setEnabled(false);
		btn6.setEnabled(false);
		btn7.setEnabled(false);
		btn8.setEnabled(false);
	}

}
