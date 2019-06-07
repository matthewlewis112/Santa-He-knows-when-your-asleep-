package edu.calpoly.lunarpower.aws;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.regions.Regions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This represents a model for login operations on AWS Mobile Hub. It manages login operations
 * such as:
 * - Sign In
 * - Sign Up
 * - Confirm Sign Up
 * - Get User Name (current signed in)
 * - Get User E-mail (current signed in)
 *
 */
@SuppressWarnings("unused")
public class AWSLoginModel {
    // constants
    private static final String ATTR_EMAIL = "email";
    private static final String SHARED_PREFERENCE = "SavedValues";
    private static final String PREFERENCE_USER_NAME = "awsUserName";
    private static final String PREFERENCE_USER_EMAIL = "awsUserEmail";
    public static final int PROCESS_SIGN_IN = 1;
    public static final int PROCESS_REGISTER = 2;
    public static final int PROCESS_CONFIRM_REGISTRATION = 3;

    // interface handler
    private AWSLoginHandler mCallback;

    // control variables
    private String userName;
    private String userPassword;
    private Context mContext;
    private CognitoUserPool mCognitoUserPool;
    private CognitoUser mCognitoUser;

    private final AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        private final SharedPreferences.Editor editor = mContext.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE).edit();
        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            // Get details of the logged user (in this case, only the e-mail)
            mCognitoUser.getDetailsInBackground(new GetDetailsHandler() {
                @Override
                public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                    // Save in SharedPreferences
                    String email = cognitoUserDetails.getAttributes().getAttributes().get(ATTR_EMAIL);
                    editor.putString(PREFERENCE_USER_EMAIL, email);
                    editor.apply();
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.e("LunarPower","",exception);
                }
            });

            // Save in SharedPreferences
            editor.putString(PREFERENCE_USER_NAME, userName);
            editor.apply();
            mCallback.onSignInSuccess();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            final AuthenticationDetails authenticationDetails = new AuthenticationDetails(userName, userPassword, null);
            authenticationContinuation.setAuthenticationDetails(authenticationDetails);
            authenticationContinuation.continueTask();
            userPassword = "";
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
            // Not implemented for this Model
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            // Not implemented for this Model
        }

        @Override
        public void onFailure(Exception exception) {
            mCallback.onFailure(PROCESS_SIGN_IN, exception);
        }
    };


    /**
     * Constructs the model for login functions in AWS Mobile Hub.
     *
     * @param context         REQUIRED: Android application context.
     * @param callback        REQUIRED: Callback handler for login operations.
     *
     */
    public AWSLoginModel(Context context, AWSLoginHandler callback) {
        mContext = context;
        IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        try{
            JSONObject myJSON = identityManager.getConfiguration().optJsonObject("CognitoUserPool");
            final String COGNITO_POOL_ID = myJSON.getString("PoolId");
            final String COGNITO_CLIENT_ID = myJSON.getString("AppClientId");
            final String COGNITO_CLIENT_SECRET = myJSON.getString("AppClientSecret");
            final String REGION = myJSON.getString("Region");
            mCognitoUserPool = new CognitoUserPool(context, COGNITO_POOL_ID, COGNITO_CLIENT_ID, COGNITO_CLIENT_SECRET, Regions.fromName(REGION));
        } catch (JSONException e) {
            Log.e("LunarPower","",e);
        }

        mCallback = callback;
    }

    /**
     * Sign in process. If succeeded, this will save the user name and e-mail in SharedPreference of
     * this context.
     *
     * This will trigger {@link AWSLoginHandler} interface defined when the constructor was called.
     *
     * @param userNameOrEmail        REQUIRED: Username or e-mail.
     * @param userPassword           REQUIRED: Password.
     */
    public void signInUser(String userNameOrEmail, String userPassword) {
        this.userName = userNameOrEmail;
        this.userPassword = userPassword;

        mCognitoUser = mCognitoUserPool.getUser(userName);
        mCognitoUser.getSessionInBackground(authenticationHandler);
    }

    /**
     * Gets the user name saved in SharedPreferences.
     *
     * @param context               REQUIRED: Android application context.
     * @return                      user name saved in SharedPreferences.
     */
    public static String getSavedUserName(Context context) {
        SharedPreferences savedValues = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
        return savedValues.getString(PREFERENCE_USER_NAME, "");
    }

    /**
     * Gets the user e-mail saved in SharedPreferences.
     *
     * @param context               REQUIRED: Android application context.
     * @return                      user e-mail saved in SharedPreferences.
     */
    public static String getSavedUserEmail(Context context) {
        SharedPreferences savedValues = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
        return savedValues.getString(PREFERENCE_USER_EMAIL, "");
    }
}