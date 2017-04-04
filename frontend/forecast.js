const Forecast = {
  view: (model, actions) =>
    h("div", { "class": "notification" },
      h("button",
        { "class": "delete", onclick: e => actions.remove(model.id) }),

      h("h2", { "class": "subtitle" },
        `# ${model.rank} ${Cities[model.id]}`),

      model.days.map(day => day.dt))
};
