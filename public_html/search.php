<html>
<head>
<meta charset="utf-8">

<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script type="text/javascript"
	src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.11.0/jquery.validate.js"></script>
<script type="text/javascript" src="./scripts/g04.js"></script>
<link rel="stylesheet" href="./styles/g04.css" type="text/css" media="screen" />

<?php
  $v = $_GET["query"]; 
  if($v) {
    $query = str_replace(' ', '%20', $v);
  	$query = str_replace('"', '%22', $query);
  	
  	//$json_url = 'http://linserv1.cims.nyu.edu:25804/search?query=' . $query . '&format=json&ranker=favorite';
  	$json_url = 'http://localhost:25804/search?query=' . $query . '&format=json&ranker=favorite';
     
    // Getting results
    $context = stream_context_create(array('http' => array('header'=>'Connection: close\r\n')));
    $result =  file_get_contents($json_url,false,$context); // Getting jSON result string
    $json_output = json_decode($result, true);
  } else {
  
  }
?>

</head>	
<body>
	<div>
		
	  <form acton="search.php" method="get" onsubmit="return isQueryNotEmpty(this);">
	    <a href="index.html"> 
		    <img align="absmiddle"  height="40" src="./image/logo.jpg"> 
	    </a>
		  <input type="text" name="query" style="width: 600; height:30" value='<?php echo $v ?>'/> 
		  <input type="submit" id="search-submit" value=""
		        style="vertical-align: top; background-image:url(image/searchbutton.png); border: solid 0px #000000; width: 70; height: 30;" />
	  </form>
	</div>
	<hr />
  <div id="main" style="padding: 10px; margin-left: 10px">
    <div id="peformance_summarny" style="margin-bottom: 5px">
      <?php 
        if($v) {
          echo $json_output["scoredDocs"]["_num_of_result"] . " results (" . $json_output["scoredDocs"]["_run_time"] . " seconds)";
        }
      ?>
    </div>
    <div id="result_list" style="width: 58%; float: left; padding-right: 5px">
	    <!-- convert Json into html format -->
	    <?php
	      if($v) {
	        echo "Showing results for <b>" . $v . "</b><br/><br/>";
	        foreach ( $json_output["scoredDocs"]["_sDocs"] as $record ) {
      ?>
        <div style="margin-bottom: 10px">
				  <font size="4em"><a href="<?php echo $record['_doc']['_url'] ?>"><?php echo $record["_doc"]["_title"]?></a></font><br/>
          <font style="color:green"><?php echo $record["_doc"]["_url"]; ?>&nbsp;[<?php echo $record["_score"]?>]</font><br/>
          <?php
            foreach ( $record["_doc"]["texts2Display"] as $snippet ) {
              echo $snippet;
            }
          ?>
        </div>
      <?php
          }
	      }
	    ?>
	  </div>
	  <div id="ad_list" style="width: 38%; float: left; padding-left: 5px">
	    <?php
	      if($v) {
	        $j=0;
	        foreach ( $json_output["scoredAdDocs"]["_sDocs"] as $record ) {
      ?>
        <div style="margin-bottom: 10px">
          <form id="f<?php echo  $j ?>" method="POST" action="log.php">
            <input type="hidden" name="url" value="<?php echo $record['_doc']['_url'] ?>" />
            <input type="hidden" name="docid" value="<?php echo $record['_doc']['_docid'] ?>" />
            <input type="hidden" name="query" value='<?php echo  $v ?>' />
          </form>
          <font size="4em"><a href="#" onclick="document.forms.f<?php echo $j?>.submit();"><?php echo $record["_doc"]["_title"]?></a></font><br/>
          <font style="color:green">
            <?php 
              $link = $record["_doc"]["_url"];
              $url = parse_url($link);
              echo $url['scheme'] . "://" . $url['host'];
            ?>&nbsp;
            [<?php echo $record["_score"]?>]
          </font><br/>
          <?php echo $record["_doc"]["keywords"] ?>
        </div>
      <?php
             $j++;
          }
	      }
	    ?>
	  </div>
  </div>
	<script language="javascript">
	  function isQueryNotEmpty(form) {
	    if(form.query.value == null || form.query.value == undefined || form.query.value.trim() == "") {
  	    return false;
	    }
	    return true;
	  }
	</script>
</body>
</html>
