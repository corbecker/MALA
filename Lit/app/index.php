<?php
require_once "../Slim/Slim.php";
require_once "../DB/DAO/UsersDAO.php";
require_once "../DB/DBManager.php";

Slim\Slim::registerAutoloader ();
$app = new \Slim\Slim ();
$databaseManager = new DBmanager ();
$accessFunctions = new UsersDAO ( $databaseManager );

//reguster a user
$app->post ( "/register/", function () use($app, $databaseManager, $accessFunctions) {
	$request = json_decode ( $app->request->getBody (), true ); 
	$databaseManager->openConnection ();
	$databaseResponse = $accessFunctions->registerUser ($request);
	$app->response->write ( json_encode ( $databaseResponse ) );
	$databaseManager->closeConnection ();
} );

//login user
$app->get("/login/:user_email/:password", function($user_email, $password) use($app, $databaseManager, $accessFunctions){
	$databaseManager->openConnection ();
	$databaseResponse = $accessFunctions->loginUser ($user_email, $password);
	$app->response->write ( json_encode ( $databaseResponse ) );
	$databaseManager->closeConnection ();
});

//get a word from saved words table
$app->post("/word/", function() use($app, $databaseManager, $accessFunctions){
	$request = json_decode ( $app->request->getBody (), true );
	$databaseManager->openConnection ();
	$databaseResponse = $accessFunctions->addWord ($request);
	$app->response->write ( json_encode ( $databaseResponse ) );
	$databaseManager->closeConnection ();
});

//delete a word
$app->delete("/remove/:user_email/:word", function($user_email, $word) use($app, $databaseManager, $accessFunctions){
	$databaseManager->openConnection ();
	$databaseResponse = $accessFunctions->removeWord ($user_email, $word);
	$app->response->write ( json_encode ( $databaseResponse ) );
	$databaseManager->closeConnection ();
});

//get a word description
$app->get("/description/:word", function($word) use($app, $databaseManager, $accessFunctions){
	$databaseManager->openConnection ();
	$databaseResponse = $accessFunctions->getWordDescription ($word);
	$app->response->write ( json_encode ( $databaseResponse ) );
	$databaseManager->closeConnection ();
});

//return all saved words for that user
$app->get("/savedwords/:user_email", function($user_email) use($app, $databaseManager, $accessFunctions){
	$databaseManager->openConnection ();
	$databaseResponse = $accessFunctions->getSavedWords ($user_email);
	$app->response->write ( json_encode ( $databaseResponse ) );
	$databaseManager->closeConnection ();
});

$app->run ();

?>