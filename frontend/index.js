const { h, app } = hyperapp;

app({
  model: "Hello, World!",
  view: model => h("p", {}, model)
});
