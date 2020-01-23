var firebaseConfig = {
apiKey : "${firebaseConfig.apiKey}",
projectId : "${projectId}",
authDomain : "${projectId}.firebaseapp.com",
databaseURL : "https://${projectId}.firebaseio.com",
storageBucket : "${projectId}.appspot.com",
appId: "${firebaseConfig.appId}"
};
firebase.initializeApp(firebaseConfig);