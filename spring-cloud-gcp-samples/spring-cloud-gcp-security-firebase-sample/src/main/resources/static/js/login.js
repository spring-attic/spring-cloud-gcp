var firebaseDemo = angular.module('firebaseDemo');

firebaseDemo.controller('login', function($scope, $http, $window, $location, userService){
    $scope.firebase = $window.firebase;

    $scope.logged = userService.isLoggedIn();
    // $window.firebase.auth().onAuthStateChanged(function(user) {
    //     if(user) {
    //         userService.setUser(user);
    //         $location.path("/");
    //     }}, function(error) {
    //     console.log(error);
    // });

    $scope.$on('user:updated', function(event, data){
        $scope.logged = userService.isLoggedIn();
    });

    $window.ui.start('#firebaseui-auth-container', $window.uiConfig);

});