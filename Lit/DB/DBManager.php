<?php
/**
 * @author Corbin
 * database manager class for mysql
 *
 */
class DBManager {

	private $connection;
	
	function __construct(){

	}
	function openConnection() {
		include_once dirname(__FILE__) . '../../conf/config.php';
		try{
			$this->connection = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME, DB_USER, DB_PASS);
		}catch(PDOException $e){
			print "Error!: " . $e->getMessage() . "<br/>";
    		die();
		}
	}

	function fetchResults($stmt){
		$rows = array (); 
		while($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
			$rows [] = $row;
		}
		return $rows;
	}

	function prepareQuery($query){
		return $this ->connection-> prepare($query);
	}

	function bindValue($stmt, $pos, $value, $type){
	 return	$stmt->bindValue($pos, $value, $type);
	
	}
	function executeQuery($stmt){
		return $stmt->execute();
	}

	

	function closeConnection(){
		$this ->connection= null;
	}
}

?>