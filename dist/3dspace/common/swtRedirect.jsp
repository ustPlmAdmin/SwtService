<%@include file="emxNavigatorInclude.inc" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
</head>
<body>
<div style="
    text-align: center;
    position: absolute;
    top: 50%;
    left: 50%;
    margin-right: -50%;
    transform: translate(-50%, -50%)">
    <h1>Generating file</h1>
    <h2>Please wait...</h2>
</div>

<script language="javascript">
    window.location.href = "<%= "/3dspace/" + emxGetParameter(request, "path") + "?objectId=" + emxGetParameter(request, "objectId")%>";
</script>

</body>
</html>