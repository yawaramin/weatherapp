const dateTimeFormat =
  new Intl.DateTimeFormat("en-CA-u-ca-iso8601", { timeZone: "UTC" });

const Forecast = {
  view: (model, actions) =>
    h("div", { "class": "notification" },
      h("button",
        { "class": "delete", onclick: e => actions.remove(model.id) }),

      h("h2", { "class": "subtitle" },
        `# ${model.rank} ${Cities[model.id]}`),

      h("div", { "class": "tile is-ancestor" },
        model.days.map(day =>
          h("div", { "class": "tile is-child notification is-vertical is-2" },
            h("p", { "class": "is-small" },
              dateTimeFormat.format(1000 * day.dt)),

            h("p", { "class": "is-small" },
              `Min: ${day.temp.min}, Max: ${day.temp.max}`)))))
};
