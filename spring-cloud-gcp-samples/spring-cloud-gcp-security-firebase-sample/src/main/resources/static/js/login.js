var firebaseDemo = angular.module('firebaseDemo');

firebaseDemo.controller('login', function($scope, $http, $window, userService){
    $scope.firebase = $window.firebase;
    $scope.logged = userService.isLoggedIn();
    $scope.firebase.auth().onAuthStateChanged(function(user) {
        if(user) {

        }}, function(error) {
        console.log(error);
    });
    $scope.login = function(){
        console.log(userService.getUser());
        userService.setUser({"name" : "John Doe"});
    }
    $scope.$on('user:updated', function(event, data){
        $scope.logged = userService.isLoggedIn();
    });
});