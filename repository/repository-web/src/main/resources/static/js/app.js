var repository = angular.module("repository", [ "ngRoute", "repositoryControllers", "repositoryDirectives",
    "swaggerUi", "smart-table", "ngAnimate","ui.bootstrap","ui.bootstrap.tpls","ui.ace"]);

repository.config([ "$routeProvider", "$httpProvider", function($routeProvider, $httpProvider) {

    $routeProvider.when("/", {
        templateUrl : "partials/search-template.html",
        controller : "SearchController",
        reloadOnSearch: false
    }).when("/import", {
        templateUrl : "partials/import-template.html",
        controller : "ImportController"
    }).when("/details/:namespace/:name/:version", {
        templateUrl : "partials/details-template.html",
        controller : "DetailsController"
    }).when("/generators", {
        templateUrl : "partials/generators-template.html",
        controller : "GeneratorController"
    }).when("/api", {
        templateUrl : "partials/swagger-template.html",
        controller : "SwaggerController"
    }).when("/login", {
        templateUrl : "partials/login-template.html",
        controller : "AuthenticateController"
    }).when("/signup", {
        templateUrl : "partials/signup-template.html",
        controller : "SignUpController"
    }).when("/settings", {
        templateUrl : "partials/settings-template.html",
        controller : "SettingsController"
    }).when("/manage", {
        templateUrl : "partials/admin-template.html",
        controller : "AdminController"
    }).otherwise({
        redirectTo : "/"
    });

    $httpProvider.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";

} ]).run(function($location, $http, $rootScope) {

    // register listener to watch route changes
    $rootScope.$on("$locationChangeStart", function(event, next, current) {
        $rootScope.error = false;
        if ($location.path() === "/upload" && $rootScope.authenticated === false) {
            $location.path("/login");
        }
    });

    $rootScope.user = [];
    $rootScope.getUser = function() {
        $http.get("./user").success(function(data, status, headers, config) {
            if (data.name !== null) {
                $rootScope.userInfo = data;
                $rootScope.user = data.name;
                $rootScope.displayName = data.displayName;
                $rootScope.authenticated = true;
                $rootScope.authority = data.role;
                if (data.isRegistered === "false") {
                    $location.path("/signup");
                }
            }
        }).error(function(data, status, headers, config) {
            $rootScope.authenticated = false;
        });
    };

    $rootScope.getUser();
    
    $rootScope.context = {
      githubEnabled: false,
      eidpEnabled: false
    };
    
    $rootScope.getContext = function() {
        $http.get("context").success(function(data, status, headers, config) {
            $rootScope.context = data;
        }).error(function(data, status, headers, config) {
            $rootScope.context = {};
        });
    };
    
    $rootScope.modelId = function(namespace,name,version) {
    	return namespace+"."+name+":"+version;
    };
    
    $rootScope.getContext();

    $rootScope.logout = function() {
        $http.post("logout", {}).success(function() {
            $rootScope.authenticated = false;
            $rootScope.userInfo = null;
            $rootScope.user = null;
            $rootScope.authority = null;
            $rootScope.getContext();
            $location.path("/login");
        }).error(function(data) {
            $rootScope.authenticated = false;
            $rootScope.userInfo = null;
            $rootScope.user = null;
            $rootScope.authority = null;
            $rootScope.getContext();
            $location.path("/login");
        });
    };
});
