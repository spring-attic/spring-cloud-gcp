var firebaseConfig = {
    apiKey : "${firebaseConfig.apiKey}",
    projectId : "${projectId}",
    authDomain : "${projectId}.firebaseapp.com",
    databaseURL : "https://${projectId}.firebaseio.com",
    storageBucket : "${projectId}.appspot.com",
    appId: "${firebaseConfig.appId}"
};

firebase.initializeApp(firebaseConfig);

var firebaseDemo = angular.module('firebaseDemo', ['ngRoute', 'ngMaterial', 'ngMessages']);

firebaseDemo.controller('MainController', ['$scope', '$window', function($scope, $window) {
        $scope.firebase = $window.firebase;
}]);

firebaseDemo.config(function ($routeProvider, $httpProvider) {
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