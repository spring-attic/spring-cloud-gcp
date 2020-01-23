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