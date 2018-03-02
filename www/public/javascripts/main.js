$(function () {
  $('.setting').on('change', 'input[type="radio"]', function (e) {
    console.log('tried to change a setting!');
    var input = e.target;
    var name = input.name;
    var value = input.id;
    var project = Delta.project.id;
    var data = {};
  
  data[name] = value === "Enabled";

    data[name] = value === "Enabled";

    $.post(project + '/settings', data, function () {
      // show flash - TBD
    });
  });

  if (window.location.search.indexOf('autorefresh') !== -1) {
    setTimeout(function () {
      window.location.reload();
    }, 5000);
  }
});
