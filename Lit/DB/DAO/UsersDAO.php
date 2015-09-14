<?php
/**
 * @author Corbin
 * Queries remote database using prepared statements and PDO
 */

class UsersDAO {
	private $databaseManager;

	function UsersDAO($databaseManager) {
		$this->databaseManager = $databaseManager;
	}

	function loginUser($user_email, $password){
		//check if that email exists in DB
		if($this->userExists($user_email)){
			$stmt = $this->databaseManager->prepareQuery("SELECT * FROM users WHERE users.user_email=? AND users.user_password=?");
			$this->databaseManager->bindValue($stmt, 1, $user_email, PDO::PARAM_STR);
			$this->databaseManager->bindValue($stmt, 2, $password, PDO::PARAM_STR);
			//execute
			$this->databaseManager->executeQuery($stmt);
			$result["status"] = 1;
			$result["content"]= $this->databaseManager->fetchResults($stmt);
			$result["message"]= "logged in";
			//user email exists in DB but if the content is empty it means password is wrong
			if(sizeof($result["content"])==0){
				$result["status"] = 0;
				$result["message"] = "Incorrect Password"; 
			}
		}else{
			$result["status"] = 0;
			$result["message"] = "User doesn't exist"; 
		}
		return $result;
	}


	//check if a user with that email already exists
	function userExists($email){
		$stmt = $this->databaseManager->prepareQuery("SELECT user_email FROM users WHERE user_email=?");
		$this->databaseManager->bindValue($stmt, 1, $email, PDO::PARAM_STR);
		$this->databaseManager->executeQuery($stmt);
		$result = $this->databaseManager->fetchResults($stmt);
		if(sizeof($result)>0){
			return true;
		}
		else{
			return false;
		}
	}

	//check if the word already exists in the table
	function wordExists($id, $word){
		$stmt = $this->databaseManager->prepareQuery("SELECT word FROM user_words WHERE user_id=? and word =?");
		$this->databaseManager->bindValue($stmt, 1, $id, PDO::PARAM_INT);
		$this->databaseManager->bindValue($stmt, 2, $word, PDO::PARAM_STR);
		$this->databaseManager->executeQuery($stmt);
		$result = $this->databaseManager->fetchResults($stmt);
		if(sizeof($result)>0){
			return true;
		}
		else{
			return false;
		}
	}

	//get the user id
	function getUserID($paramArray){
		$stmt = $this->databaseManager->prepareQuery("SELECT * FROM users WHERE user_email=?");
		$this->databaseManager->bindValue($stmt, 1, $paramArray["user_email"], PDO::PARAM_STR);
		$this->databaseManager->executeQuery($stmt);
		$result = $this->databaseManager->fetchResults($stmt);
		return $result;
	}

	//get user id
	function getID($user_email){
		$stmt = $this->databaseManager->prepareQuery("SELECT * FROM users WHERE user_email=?");
		$this->databaseManager->bindValue($stmt, 1, $user_email, PDO::PARAM_STR);
		$this->databaseManager->executeQuery($stmt);
		$result = $this->databaseManager->fetchResults($stmt);
		return $result;
	}

	//get all saved words from database
	function getSavedWords($user_email){
		$uid = $this->getID($user_email);
		$stmt = $this->databaseManager->prepareQuery("SELECT * FROM user_words WHERE user_id=?");
		$this->databaseManager->bindValue($stmt, 1, $uid[0]["user_id"], PDO::PARAM_STR);
		$this->databaseManager->executeQuery($stmt);
		$result = $this->databaseManager->fetchResults($stmt);
		return $result;
	}

	//get a word description from dictionary table
	function getWordDescription($word){
		$stmt = $this->databaseManager->prepareQuery("SELECT definition FROM dictionary WHERE word=?");
		$this->databaseManager->bindValue($stmt, 1, $word, PDO::PARAM_STR);
		$this->databaseManager->executeQuery($stmt);
		$result = $this->databaseManager->fetchResults($stmt);
		return $result;
	}

	//register a new user into the database
	function registerUser($paramArray) {
		if($this->userExists($paramArray["user_email"])){
			$result["status"] = 0;
			$result["message"] = "User already exists";
		}else{
			$stmt = $this->databaseManager->prepareQuery("INSERT INTO users (user_email, user_name,
			 user_password) VALUES (?, ?, ?)");
			$this->databaseManager->bindValue($stmt, 1, $paramArray['user_email'], PDO::PARAM_STR);
			$this->databaseManager->bindValue($stmt, 2, $paramArray['user_name'], PDO::PARAM_STR);
			$this->databaseManager->bindValue($stmt, 3, $paramArray['user_password'], PDO::PARAM_STR);

			$this->databaseManager->executeQuery($stmt);
			$result["status"] = 1;
			$result["message"] = "User created";
		}
		return $result;
	}

	//add a new word
	function addWord($paramArray) {
		if($this->userExists($paramArray["user_email"])){
			$uid = $this->getUserID($paramArray);
			if(!$this->wordExists($uid[0]["user_id"], $paramArray["word"])){
				$stmt = $this->databaseManager->prepareQuery("INSERT INTO user_words (user_id, word) VALUES (?, ?)");
				$this->databaseManager->bindValue($stmt, 1, $uid[0]["user_id"], PDO::PARAM_INT);
				$this->databaseManager->bindValue($stmt, 2, $paramArray['word'], PDO::PARAM_STR);
				//execute
				$this->databaseManager->executeQuery($stmt);
				$result["status"] = 1;
				$result["message"] = "Word Added";
			}
		}
		return $result;

	}

	//remove a word from the database
	function removeWord($user_email, $word) {
		if($this->userExists($user_email)){
			$uid = $this->getID($user_email);
			$stmt = $this->databaseManager->prepareQuery("DELETE FROM user_words WHERE user_id = ? AND word = ?");
			$this->databaseManager->bindValue($stmt, 1, $uid[0]["user_id"], PDO::PARAM_INT);
			$this->databaseManager->bindValue($stmt, 2, $word, PDO::PARAM_STR);
			//execute
			$this->databaseManager->executeQuery($stmt);
			$result["status"] = 1;
			$result["message"] = "Word Deleted";
		}
		return $result;
		
	}


}
?>
