package com.noelportugal.nymiexample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bionym.ncl.Ncl;
import com.bionym.ncl.NclCallback;
import com.bionym.ncl.NclEvent;
import com.bionym.ncl.NclEventInit;
import com.bionym.ncl.NclMode;
import com.bionym.ncl.NclProvision;
import com.noelportugal.nymiexample.R;

import java.util.Arrays;

public class MainActivity extends Activity implements ProvisionController.ProvisionProcessListener,
                                                ValidationController.ValidationProcessListener {
	static final String LOG_TAG = "AndroidExample";

    static boolean nclInitialized = false;
    
    Button startProvision, startValidation, disconnect;

    ProvisionController provisionController;
    ValidationController valiationController;
    boolean connectNymi = true;

    int nymiHandle = Ncl.NYMI_HANDLE_ANY;
    NclProvision provision;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        startProvision = (Button) findViewById(R.id.provision);
        startValidation = (Button) findViewById(R.id.validation);
		disconnect = (Button) findViewById(R.id.disconnect);
		
        startProvision.setOnClickListener(new View.OnClickListener() {
        	long lastClickTime; // for double click protection
            @Override
            public void onClick(View v) {
	    		if (System.currentTimeMillis() - lastClickTime >= 1000) { // double click protection
	    			lastClickTime = System.currentTimeMillis();
	    			initializeNcl();
	    			nymiHandle = -1;
	    			if (provisionController == null) {
	    				provisionController = new ProvisionController(MainActivity.this);
	    			}else {
	            		provisionController.stop();
	    			}
	    			provisionController.startProvision(MainActivity.this);
	    		}
            }
        });

        startValidation.setOnClickListener(new View.OnClickListener() {
        	long lastClickTime;
            @Override
            public void onClick(View v) {
            	if (System.currentTimeMillis() - lastClickTime >= 1000) { // double click protection
        			lastClickTime = System.currentTimeMillis();
        			startProvision.setEnabled(false);
        			if (valiationController == null) {
        				valiationController = new ValidationController(MainActivity.this);
        			}else {
        				valiationController.stop();
        			}
	                valiationController.startValidation(MainActivity.this, provisionController.getProvision());
            	}
            }
        });
        
        disconnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (nymiHandle >= 0) {
					disconnect.setEnabled(false);
	                startValidation.setEnabled(true);
	                startProvision.setEnabled(true);
					Ncl.disconnect(nymiHandle);
					nymiHandle = -1;
				}
			}
		});
    }

    @Override
	protected void onPause() {
	    	if (provisionController != null) {
	    		provisionController.stop();
	    	}
	    	
	    	if (valiationController != null) {
	    		valiationController.stop();
	    	}
		super.onPause();
	}

	@Override
    protected void onStop() {
        if (nclInitialized && nymiHandle >= 0) {
            Ncl.disconnect(nymiHandle);
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    /**
     * Initialize the NCL library
     */
    protected void initializeNcl() {
        if (!nclInitialized) {
            if (connectNymi) {
                initializeNclForNymiBand();
            }
        }
    }


    /**
     * Initialize NCL library for connecting to a Nymi Band
     * @return true if the library is initialized
     */
    protected boolean initializeNclForNymiBand() {
        if (!nclInitialized) {
	    	NclCallback nclCallback = new MyNclCallback();
            boolean result = Ncl.init(nclCallback, null, "NCLExample", NclMode.NCL_MODE_DEFAULT, this);

            if (!result) { // failed to initialize NCL
                Toast.makeText(MainActivity.this, "Failed to initialize NCL library!", Toast.LENGTH_LONG).show();
                return false;
            }
            nclInitialized = true;
        }
        return true;
    }

    @Override
    public void onStartProcess(ProvisionController controller) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Nymi start provision ..", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onAgreement(final ProvisionController controller) {
        nymiHandle = controller.getNymiHandle();
        controller.accept();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Agree on pattern: " + Arrays.toString(controller.getLedPatterns()), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onProvisioned(final ProvisionController controller) {
    	nymiHandle = controller.getNymiHandle();
        provision = controller.getProvision();
		controller.stop();
        runOnUiThread(new Runnable() {
	        @Override
	        public void run() {
	    		startProvision.setEnabled(false);
	    		startValidation.setEnabled(true);
	            Toast.makeText(MainActivity.this, "Nymi provisioned: " + Arrays.toString(provision.id.v), Toast.LENGTH_LONG).show();
	        }
        });
    }

    @Override
    public void onFailure(ProvisionController controller) {
		controller.stop();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Nymi provision failed!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDisconnected(ProvisionController controller) {
		controller.stop();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
	        	startValidation.setEnabled(provision != null);
	        	disconnect.setEnabled(false);
                Toast.makeText(MainActivity.this, "Nymi disconnected: " + provision, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStartProcess(ValidationController controller) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Nymi start validation for: " + Arrays.toString(provision.id.v), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onFound(ValidationController controller) {
        nymiHandle = controller.getNymiHandle();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Nymi validation found Nymi on: " + Arrays.toString(provision.id.v), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onValidated(ValidationController controller) {
		nymiHandle = controller.getNymiHandle();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            		startValidation.setEnabled(false);
            		disconnect.setEnabled(true);
	            Toast.makeText(MainActivity.this, "Nymi validated!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onFailure(ValidationController controller) {
		controller.stop();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Nymi validated failed!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDisconnected(ValidationController controller) {
		controller.stop();
    		runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	disconnect.setEnabled(false);
                startValidation.setEnabled(true);
                startProvision.setEnabled(true);
                Toast.makeText(MainActivity.this, "Nymi disconnected: " + provision, Toast.LENGTH_LONG).show();
            }
        });
    }
    
	/**
	 * Callback for NclEventInit
	 *
	 */
	class MyNclCallback implements NclCallback {
		@Override
		public void call(NclEvent event, Object userData) {
			Log.d(LOG_TAG, this.toString() + ": " + event.getClass().getName());
			if (event instanceof NclEventInit) {
	            if (!((NclEventInit) event).success) {
	                runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                        Toast.makeText(MainActivity.this, "Failed to initialize NCL library!", Toast.LENGTH_LONG).show();
	                    }
	                });
	            }
	        }
		}
	}
}
