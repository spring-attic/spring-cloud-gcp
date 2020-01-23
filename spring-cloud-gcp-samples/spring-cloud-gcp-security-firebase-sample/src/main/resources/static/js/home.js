var firebaseDemo = angular.module('firebaseDemo');

firebaseDemo.controller('home', function($scope, $http, userService){
    $scope.answer = '';
    $scope.status = 0;
    $scope.ask = function () {
        var req = {
            method: 'GET',
            url : '/answer',
            headers : {
                'Authorization' : 'Bearer ' + userToken()
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
    function userToken() {
        if(userService.user == null){
            return '';
        }else{
            return userService.user.token;
        }
    }
});