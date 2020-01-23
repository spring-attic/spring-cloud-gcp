var firebaseConfig = {
    apiKey : "${firebaseConfig.apiKey}",
    projectId : "${projectId}",
    authDomain : "${projectId}.firebaseapp.com",
    databaseURL : "https://${projectId}.firebaseio.com",
    storageBucket : "${projectId}.appspot.com",
    appId: "${firebaseConfig.appId}"
};

var firebaseUser;

firebase.initializeApp(firebaseConfig);
var uiConfig = {
                signInSuccessUrl: "http://"+window.location.hostname+":"+window.location.port+"/",
                signInOptions: [
                    firebase.auth.GoogleAuthProvider.PROVIDER_ID,
                    firebase.auth.EmailAuthProvider.PROVIDER_ID
                ],
                tosUrl: 'http://localhost:8080/',

                privacyPolicyUrl: function() {
                    window.location.assign("http://"+window.location.hostname+":"+window.location.port+"/");
                    }
                };

firebase.auth().onAuthStateChanged(function(user) {
            if(user) {
                if(firebaseUser == null){
                    firebaseUser = user;
                    //window.location.assign("http://"+window.location.hostname+":"+window.location.port+"/#!/");
                }

            }}, function(error) {
            console.log(error);
        });

var ui = new firebaseui.auth.AuthUI(firebase.auth());

var firebaseDemo = angular.module('firebaseDemo', ['ngRoute', 'ngMaterial', 'ngMessages']);

firebaseDemo.controller('MainController', function($scope, $window, userService) {
        $scope.firebase = $window.firebase;
        $scope.logged = userService.isLoggedIn();
        $scope.$on('user:updated', function(event,data){
            $scope.logged = userService.isLoggedIn();
        });

});

firebaseDemo.config(function ($routeProvider, $httpProvider, $locationProvider) {
    $routeProvider
        .when('/',{
            templateUrl: 'views/home.html',
            controller: 'home'
        })
        .when('/login',{
            templateUrl: 'views/login.html',
            controller: 'login'
        })
    $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';
});

firebaseDemo.factory('userService',[ '$rootScope', function($rootScope) {
    var user;
    return {
        getUser: getUser,
        setUser: setUser,
        isLoggedIn: isLoggedIn
    }

    function getUser(){
        return user;
    }

    function setUser(data){
        user = data;
        $rootScope.$broadcast('user:updated', data);
    }

    function isLoggedIn() {
        return user != null;
    }

}]);