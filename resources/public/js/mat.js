$(function(){
    $(document).mousemove(function(e){
        var aMap=$("#map");
	$("#position").css("visibility",false);
       // $("#position").css("top",e.pageY+gridSize-(e.pageY%gridSize)-offsetY).css("left",e.pageX-(e.pageX%gridSize)-offsetX);
               });
    $(".token").draggable({ grid: [gridSize,gridSize],
                          stop: function (event, ui) {
			      var aMap=$("#map");
			      var posX=$(this).offset().left-aMap.offset().left;
			      posX=(posX-posX%gridSize)/gridSize;
			      var posY=$(this).offset().top-aMap.offset().top;
			      posY=(posY-posY%gridSize)/gridSize;
			      $.post("/combat/"+combatName+"/move",
				     {name: "Celeste", posx: posX, posy: posY});
}})}
)
