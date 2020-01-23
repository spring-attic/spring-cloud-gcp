var firebaseDemo = angular.module('firebaseDemo');

firebaseDemo.controller('login', function($scope, $http, $window){
    $scope.firebase = $window.firebase;
    $scope.firebase.auth().onAuthStateChanged(function(user) {
        if(user) {

        }}, function(error) {
        console.log(error);
    });
});