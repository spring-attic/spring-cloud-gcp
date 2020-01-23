var firebaseDemo = angular.module('firebaseDemo');

firebaseDemo.controller('home', function($scope, $http, $window, userService){
    $scope.answer = '';
    $scope.status = 0;
    $scope.token = '';
    if($window.firebaseUser != null){
        userService.setUser($window.firebaseUser);
    }
    $scope.ask = function () {
        var req = {
            method: 'GET',
            url : '/answer',
            headers : {
                'Authorization' : 'Bearer ' + $scope.token
            }
        };
        $http(req).then(function success(response){
            $scope.status = response.status;
            $scope.answer = response.data.answer;
        },
         function error(response){
            $scope.answer = "Ooops, it looks like you have not identified yourself yet, please be kind and login first.";
            $scope.status = response.status;
        });
    }
    $scope.$on('user:updated', function(event, data){
        userService.getUser().getIdToken().then( function(accessToken){
            $scope.token = accessToken;
        } , null, ' ');
    });
});