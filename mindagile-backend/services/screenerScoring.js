/**
 * PHQ-9 and GAD-7 scoring (COCM-style cutoffs).
 * Answer scale 0–3; positive screen triggers staff notifications.
 */

// PHQ-9: Over the last 2 weeks, how often have you been bothered by... (0–3)
const PHQ9_QUESTIONS = [
  'Little interest or pleasure in doing things',
  'Feeling down, depressed, or hopeless',
  'Trouble falling or staying asleep, or sleeping too much',
  'Feeling tired or having little energy',
  'Poor appetite or overeating',
  'Feeling bad about yourself—or that you are a failure or have let yourself or your family down',
  'Trouble concentrating on things, such as reading the newspaper or watching television',
  'Moving or speaking so slowly that other people could have noticed, or the opposite—being so fidgety or restless that you have been moving around a lot more than usual',
  'Thoughts that you would be better off dead or of hurting yourself in some way'
];

// GAD-7: Over the last 2 weeks, how often have you been bothered by... (0–3)
const GAD7_QUESTIONS = [
  'Feeling nervous, anxious, or on edge',
  'Not being able to stop or control worrying',
  'Worrying too much about different things',
  'Trouble relaxing',
  'Being so restless that it\'s hard to sit still',
  'Becoming easily annoyed or irritable',
  'Feeling afraid, as if something awful might happen'
];

/**
 * @param {number[]} answers - Array of 9 values 0–3 (by question index)
 * @returns {{ total: number, severity: string, positive: boolean, details: object }}
 */
function scorePHQ9(answers) {
  if (!Array.isArray(answers) || answers.length < 9) {
    return { total: 0, severity: 'Minimal', positive: false, details: {} };
  }
  const total = answers.slice(0, 9).reduce((sum, v) => sum + (Number(v) || 0), 0);
  let severity = 'Minimal';
  if (total <= 4) severity = 'Minimal';
  else if (total <= 9) severity = 'Mild';
  else if (total <= 14) severity = 'Moderate';
  else if (total <= 19) severity = 'Moderately Severe';
  else severity = 'Severe';

  const item9 = Number(answers[8]) || 0;
  const positive = total >= 10 || item9 >= 1;
  return { total, severity, positive, details: { item9 } };
}

/**
 * @param {number[]} answers - Array of 7 values 0–3
 * @returns {{ total: number, severity: string, positive: boolean, details: object }}
 */
function scoreGAD7(answers) {
  if (!Array.isArray(answers) || answers.length < 7) {
    return { total: 0, severity: 'Minimal', positive: false, details: {} };
  }
  const total = answers.slice(0, 7).reduce((sum, v) => sum + (Number(v) || 0), 0);
  let severity = 'Minimal';
  if (total <= 4) severity = 'Minimal';
  else if (total <= 9) severity = 'Mild';
  else if (total <= 14) severity = 'Moderate';
  else severity = 'Severe';

  const positive = total >= 10;
  return { total, severity, positive, details: {} };
}

function getPHQ9Questions() {
  return [...PHQ9_QUESTIONS];
}

function getGAD7Questions() {
  return [...GAD7_QUESTIONS];
}

module.exports = {
  PHQ9_QUESTIONS,
  GAD7_QUESTIONS,
  scorePHQ9,
  scoreGAD7,
  getPHQ9Questions,
  getGAD7Questions
};
