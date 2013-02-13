package com.android.launcher2;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.launcher.R;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class HomescreenConfigurationReceiver extends BroadcastReceiver {

	public static final String OPTIONS_TYPE = "type";
	public static final String OPTIONS_TITLE = "title";//Not used
	public static final String OPTIONS_PACKAGE_NAME = "packageName";
	public static final String OPTIONS_CLASS_NAME = "className";
	public static final String OPTIONS_CONTAINER = "container";
	public static final String OPTIONS_SCREEN = "screen";
	public static final String OPTIONS_X = "x";
	public static final String OPTIONS_Y = "y";
	public static final String OPTIONS_ROWS = "rows";
	public static final String OPTIONS_COLUMNS = "cols";
	public static final String ITEM_TYPE_WIDGET = "widget";
	public static final String ITEM_TYPE_SHORTCUT = "shortcut";

	/**
	 * Version of this API
	 */
	public static final String OPTIONS_VERSION = "version";

	/**
	 * The protocol version of this service is incorrect
	 */
	public static final String RESULT_INVALID_VERSION = "RESULT_INVALID_VERSION";

	public static final String RESULT_MISSING_REQUIRED_PARAMETER = "RESULT_MISSING_REQUIRED_PARAMETER";

	public static final String RESULT_INVALID_JSON = "RESULT_INVALID_JSON";

	/**
	 * Unknown email failure
	 */
	public static final String RESULT_UNKNOWN = "RESULT_UNKNOWN";

	/**
	 * Email account creation success
	 */
	public static final int RESULT_CODE_SUCCESS = 0x0;

	/**
	 * Email account creation failure
	 */
	public static final int RESULT_CODE_FAILURE = 0x1;

	private static final Float SUPPORTED_VERSION = 1.0f;//TODO: supported version


	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("Homescreen", "starting configuration");
//		if (!isVersionProtocolSupported(intent.getExtras())) {
//			sendResponse(context, null, false, RESULT_INVALID_VERSION);
//			return;
//		}

		List<Bundle> bundles = null;
		if (intent.hasExtra("homescreen")) {
			bundles = fromIntentToBundles(intent);

			if(bundles.isEmpty()) {
				sendResponse(context, null, false, RESULT_INVALID_JSON);
			}
		} else {
			sendResponse(context, null, false, RESULT_MISSING_REQUIRED_PARAMETER);
			return;
		}


		boolean hasError = false;

		JSONArray messages = new JSONArray();

		for(Bundle options : bundles) {
            JSONObject payload = null;
            try {
                payload = new JSONObject(options.getString( "jsonObject" ));
            } catch ( JSONException e ) {
                e.printStackTrace();
            }

			if (hasRequiredOptions(payload)) {
				int containerType = options.getInt(OPTIONS_CONTAINER) > 0 ? LauncherSettings.Favorites.CONTAINER_DESKTOP
						: LauncherSettings.Favorites.CONTAINER_HOTSEAT;
				String itemType = null;
				try {
                    itemType = payload.getString( "type" );
                } catch ( JSONException e1 ) {
                    e1.printStackTrace();
                }

				ComponentName cn = new ComponentName(options.getString(OPTIONS_PACKAGE_NAME)
						, options.getString(OPTIONS_CLASS_NAME));

				if (itemType.equals( ITEM_TYPE_WIDGET ) ) {
				    installWidget(context, cn, options.getInt(OPTIONS_SCREEN, -1),
	                        containerType, options.getInt(OPTIONS_X, -1),
	                        options.getInt(OPTIONS_Y, -1),
	                        options.getInt(OPTIONS_ROWS, -1),
	                        options.getInt(OPTIONS_COLUMNS, -1), true);
				} else if (itemType.equals( ITEM_TYPE_SHORTCUT ) ) {
				    installShortCut( context, cn, options.getInt(OPTIONS_SCREEN, -1), containerType, options.getInt(OPTIONS_X, -1), options.getInt(OPTIONS_Y, -1) );
				}


				messages.put(buildResponse(options, true, 0, "OK"));
			} else {
				hasError = true;
				messages.put(buildResponse(options, true, 0, RESULT_MISSING_REQUIRED_PARAMETER));
			}			
		}

		sendResponse(context, messages, hasError, null);
		}


	private void sendResponse(Context context, JSONArray items, boolean isSuccess, String errorMessage) {
		Intent result = new Intent(
				"com.android.homescreen.CONFIGURE_HOMESCREEN_RESULT");
		result.putExtra("success", isSuccess);		
		result.putExtra("version", "1.0");//TODO - hard-coded
		result.putExtra("intent", "com.android.homescreen.CONFIGURE_HOMESCREEN");

		if (items != null) {
			Log.d("Homescreen", items.toString());
			result.putExtra("homescreen", items.toString());		
		} else {
			result.putExtra("errorCode", 0);
			if(errorMessage != null) {
				result.putExtra("errorMessage", errorMessage);	
				Log.i("Homescreen", errorMessage);
			}
		}


		context.sendBroadcast(result);
	}

	private JSONObject buildResponse(Bundle options, boolean isSuccess, int errorCode, String errorMessage) {

		try {
			JSONObject payload = new JSONObject(options.getString("jsonObject"));
			payload.put("success", (isSuccess ? RESULT_CODE_SUCCESS : RESULT_CODE_FAILURE) );
			payload.put("errorCode", errorCode);
			payload.put("errorMessage", errorMessage);
			return payload;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean hasRequiredOptions(JSONObject options) {
		boolean hasOptions = options.has(OPTIONS_TYPE)
				&& options.has(OPTIONS_PACKAGE_NAME)
				&& options.has(OPTIONS_CLASS_NAME)
				&& options.has(OPTIONS_CONTAINER);

		try {
            if(hasOptions && options.getInt(OPTIONS_CONTAINER) > 0 ) {
            	 return options.has(OPTIONS_SCREEN);
            }
        } catch ( JSONException e ) {
            e.printStackTrace();
        }

		return hasOptions;
	}

	private List<Bundle> fromIntentToBundles(Intent intent) {
		try {
			JSONArray bundledArray = new JSONArray(
					intent.getStringExtra("homescreen"));
			Log.d("fromIntentToBundles", "bundledArray = " + bundledArray.toString());
			// TODO: better to use homescreens
			if (bundledArray != null) {
				return fromJsonToBundles(bundledArray);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return new ArrayList<Bundle>();
	}


 	private List<Bundle> fromJsonToBundles(JSONArray items) {
		ArrayList<Bundle> bundles = new ArrayList<Bundle>();

	        try {
	            for ( int index = 0; index < items.length(); index++ ) {
	                JSONObject item = items.getJSONObject( index );
	                Bundle bundle = new Bundle();

	                bundle.putString("jsonObject", item.toString());
	                addOptionString(OPTIONS_TYPE, item, bundle);
	                addOptionString(OPTIONS_PACKAGE_NAME, item, bundle);                
	                addOptionString(OPTIONS_CLASS_NAME, item, bundle);

	                addOptionInt(OPTIONS_CONTAINER, item, bundle);
	                addOptionInt(OPTIONS_SCREEN, item, bundle);
	                addOptionInt(OPTIONS_X, item, bundle);
	                addOptionInt(OPTIONS_Y, item, bundle);
	                addOptionInt(OPTIONS_ROWS, item, bundle);
	                addOptionInt(OPTIONS_COLUMNS, item, bundle);

	                bundles.add(bundle);
	            }
	        } catch ( JSONException je ) {
	        	je.printStackTrace();
	        }
	        Log.d("Homescreen", "fromJsonToBundles.bundles = " + bundles.toString());
		return bundles;

	}

	private void addOptionString(String option, JSONObject item, Bundle bundle ) {
        if(!item.isNull(option)) {
        	try {
				bundle.putString(option, item.getString(option));
			} catch (JSONException e) {
			}
        }     
	}

	private void addOptionInt(String option, JSONObject item, Bundle bundle ) {
        if(!item.isNull(option)) {
        	try {
				bundle.putInt(option, item.getInt(option));
			} catch (JSONException e) {
			}
        }     
	}

	private LauncherAppWidgetInfo installWidget(Context context, ComponentName cn, int screen,
			int container, int xCoOd, int yCoOd, int spanX, int spanY,
			boolean notify) {
		LauncherApplication app = (LauncherApplication) context
				.getApplicationContext();		
		return app.getModel().addAppWidget(context, cn, 
				container, screen, xCoOd, yCoOd, spanX, spanY, true);
	}

	/**
	 * Returns true if version protocol is supported, otherwise false
	 * 
	 * @param options
	 *            email account options
	 * @return true if version protocol is supported, otherwise false
	 */
	private static boolean isVersionProtocolSupported(Bundle options) {
		// TODO: Wouldn't this be better as a float: options.getFloat?
		try {
			Float version = Float.valueOf(options.getString(OPTIONS_VERSION,
					"0.0"));
			return !version.equals(0.0f) && (version <= SUPPORTED_VERSION);
		} catch (NumberFormatException e) {
			return false;
		}
	}


	private boolean installShortCut(Context context, ComponentName cn, int screen, int container, int xCoOd, int yCoOd) {    

	    String name = getAppName(context, cn.getPackageName());
	    Drawable icon = getAppIcon(context, cn);
        BitmapDrawable bd = (BitmapDrawable) icon;
	    Intent shortcutIntent = new Intent();
        shortcutIntent.setClassName(cn.getPackageName(), cn.getClassName());
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        Intent data = new Intent();
        data.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        data.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        data.putExtra(Intent.EXTRA_SHORTCUT_ICON, bd.getBitmap());

        if (findEmptyCell(context, new int[] {xCoOd, yCoOd}, screen)) {
            if (shortcutIntent != null) {
                if (shortcutIntent.getAction() == null) {
                    shortcutIntent.setAction(Intent.ACTION_VIEW);
                }

                // By default, we allow for duplicate entries (located in
                // different places)
                 boolean duplicate = data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);
                if ( duplicate || !LauncherModel.shortcutExists(context, name, shortcutIntent)) {
                    LauncherApplication app = (LauncherApplication) context.getApplicationContext();
                    ShortcutInfo info;
                    if (xCoOd == -1 && yCoOd == -1) {
                        info = app.getModel().addShortcut(context, data,
                            LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, xCoOd,
                            yCoOd, true);
                    } else {
                        info = app.getModel().addShortcut(context, data,
                                LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, xCoOd,
                                yCoOd, true);
                    }
                    if (info != null) {
                        Toast.makeText(context, context.getString(R.string.shortcut_installed, name),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        return false;
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.shortcut_duplicate, name),
                            Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        } else {
            Toast.makeText(context, context.getString(R.string.out_of_space),
                    Toast.LENGTH_SHORT).show();
        }

        return false; 

	}

	private static boolean findEmptyCell(Context context, int[] xy, int screen) {
        final int xCount = LauncherModel.getCellCountX();
        final int yCount = LauncherModel.getCellCountY();
        boolean[][] occupied = new boolean[xCount][yCount];

        ArrayList<ItemInfo> items = LauncherModel.getItemsInLocalCoordinates(context);
        ItemInfo item = null;
        int cellX, cellY, spanX, spanY;
        for (int i = 0; i < items.size(); ++i) {
            item = items.get(i);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (item.screen == screen) {
                    cellX = item.cellX;
                    cellY = item.cellY;
                    spanX = item.spanX;
                    spanY = item.spanY;
                    for (int x = cellX; x < cellX + spanX && x < xCount; x++) {
                        for (int y = cellY; y < cellY + spanY && y < yCount; y++) {
                            occupied[x][y] = true;
                        }
                    }
                }
            }
        }

        return CellLayout.findVacantCell(xy, 1, 1, xCount, yCount, occupied);
    }

	private String getAppName(Context context, String packageName) {
	    final PackageManager pm = context.getPackageManager();
	    android.content.pm.ApplicationInfo ai;
	    try {
	        ai = pm.getApplicationInfo(packageName, 0);
	    } catch (final NameNotFoundException e) {
	        ai = null;
	    }
	    return ( String ) ( ai != null ? pm.getApplicationLabel(ai) : "(unknown)" );
	}

	private Drawable getAppIcon(Context context, ComponentName cn) {
	    final PackageManager pm = context.getPackageManager();
	    try {
            return pm.getActivityIcon(cn);
        } catch ( NameNotFoundException e ) {
            e.printStackTrace();
            return null;
        }
	}
}