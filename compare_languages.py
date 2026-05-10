import json
import os
import re
from datetime import datetime

def load_results(path):
    if not os.path.exists(path):
        return None
    with open(path, 'r') as f:
        return json.load(f)

def clean_id(name):
    return re.sub(r'[^a-zA-Z0-9_]', '', name.replace(' ', '_'))

def generate_comparison_report(java_data, python_data, output_dir):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    java_tasks = {t['taskName']: {cl['concurrency']: cl for cl in t['concurrencyLevels']} for t in java_data['tasks']} if java_data else {}
    python_tasks = {t['taskName']: {cl['concurrency']: cl for cl in t['concurrencyLevels']} for t in python_data['tasks']} if python_data else {}
    
    all_task_names = sorted(set(java_tasks.keys()) | set(python_tasks.keys()))
    
    html = [
        "<!DOCTYPE html>",
        "<html lang='en'>",
        "<head>",
        "    <meta charset='UTF-8'>",
        "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>",
        "    <title>Java vs Python Performance Comparison</title>",
        "    <script src='https://cdn.jsdelivr.net/npm/chart.js'></script>",
        "    <style>",
        "        :root {",
        "            --primary: #1a1a2e;",
        "            --secondary: #16213e;",
        "            --accent: #0f3460;",
        "            --java-color: #f89820;",
        "            --python-color: #3776ab;",
        "            --text: #e2e8f0;",
        "            --bg: #0a0a0f;",
        "        }",
        "        * { box-sizing: border-box; margin: 0; padding: 0; }",
        "        body { font-family: 'Inter', system-ui, sans-serif; background: var(--bg); color: var(--text); line-height: 1.6; }",
        "        .container { max-width: 1200px; margin: 0 auto; padding: 40px 20px; }",
        "        header { ",
        "            background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);",
        "            padding: 50px; border-radius: 20px; margin-bottom: 40px; text-align: center;",
        "            box-shadow: 0 10px 30px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.05);",
        "        }",
        "        h1 { font-size: 2.5rem; margin-bottom: 10px; background: linear-gradient(to right, #fff, #94a3b8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }",
        "        .subtitle { color: #94a3b8; font-size: 1.1rem; }",
        "        .timestamp { display: inline-block; margin-top: 20px; padding: 5px 15px; background: rgba(255,255,255,0.05); border-radius: 20px; font-size: 0.85rem; }",
        "        ",
        "        .comparison-grid { display: grid; grid-template-columns: 1fr; gap: 40px; }",
        "        .task-card { ",
        "            background: #151521; border-radius: 16px; padding: 30px; border: 1px solid rgba(255,255,255,0.05);",
        "        }",
        "        h2 { font-size: 1.4rem; margin-bottom: 25px; color: #fff; display: flex; align-items: center; }",
        "        h2::before { content: ''; display: inline-block; width: 4px; height: 24px; background: var(--accent); margin-right: 12px; border-radius: 2px; }",
        "        ",
        "        .charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }",
        "        .chart-container { background: rgba(0,0,0,0.2); padding: 15px; border-radius: 8px; }",
        "        canvas { width: 100% !important; height: 300px !important; }",
        "        ",
        "        footer { text-align: center; margin-top: 60px; color: #475569; font-size: 0.9rem; }",
        "    </style>",
        "</head>",
        "<body>",
        "    <div class='container'>",
        "        <header>",
        "            <h1>Language Performance Comparison</h1>",
        "            <div class='subtitle'>Java vs Python Concurrent Execution</div>",
        "            <div class='timestamp'>Generated: " + timestamp + "</div>",
        "        </header>",
        "        <div class='comparison-grid'>"
    ]

    scripts = []

    for idx, task_name in enumerate(all_task_names):
        j_levels = java_tasks.get(task_name, {})
        p_levels = python_tasks.get(task_name, {})
        
        all_concurrencies = sorted(set(j_levels.keys()) | set(p_levels.keys()))
        
        j_tp, p_tp = [], []
        j_lat, p_lat = [], []
        j_cpu, p_cpu = [], []
        j_mem, p_mem = [], []
        labels = []

        for conc in all_concurrencies:
            labels.append(str(conc))
            j = j_levels.get(conc)
            p = p_levels.get(conc)
            j_tp.append(j['throughputOpsPerSec'] if j else None)
            p_tp.append(p['throughputOpsPerSec'] if p else None)
            j_lat.append(j['avgLatencyMs'] if j else None)
            p_lat.append(p['avgLatencyMs'] if p else None)
            j_cpu.append(j['avgCpuPercent'] if j else None)
            p_cpu.append(p['avgCpuPercent'] if p else None)
            j_mem.append(j['peakMemoryMB'] if j else None)
            p_mem.append(p['peakMemoryMB'] if p else None)

        task_id = f"task_{idx}"
        
        html.append(f"<div class='task-card'>")
        html.append(f"<h2>{task_name}</h2>")
        html.append("<div class='charts-grid'>")
        
        metrics = [
            ("Throughput (ops/s)", "tp", j_tp, p_tp),
            ("Avg Latency (ms)", "lat", j_lat, p_lat),
            ("CPU Usage (%)", "cpu", j_cpu, p_cpu),
            ("Peak Memory (MB)", "mem", j_mem, p_mem)
        ]

        for m_name, m_id, j_data, p_data in metrics:
            cid = f"chart_{task_id}_{m_id}"
            html.append(f"<div class='chart-container'><canvas id='{cid}'></canvas></div>")
            
            scripts.append(f"""
            new Chart(document.getElementById('{cid}').getContext('2d'), {{
                type: 'line',
                data: {{
                    labels: {json.dumps(labels)},
                    datasets: [
                        {{ label: 'Java', data: {json.dumps(j_data)}, borderColor: '#f89820', backgroundColor: 'rgba(248, 152, 32, 0.1)', tension: 0.1, fill: true }},
                        {{ label: 'Python', data: {json.dumps(p_data)}, borderColor: '#3776ab', backgroundColor: 'rgba(55, 118, 171, 0.1)', tension: 0.1, fill: true }}
                    ]
                }},
                options: {{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {{
                        title: {{ display: true, text: '{m_name}', color: '#e2e8f0' }},
                        legend: {{ labels: {{ color: '#94a3b8' }} }}
                    }},
                    scales: {{
                        x: {{ title: {{ display: true, text: 'Threads', color: '#94a3b8' }}, ticks: {{ color: '#94a3b8' }}, grid: {{ color: 'rgba(255,255,255,0.05)' }} }},
                        y: {{ ticks: {{ color: '#94a3b8' }}, grid: {{ color: 'rgba(255,255,255,0.05)' }} }}
                    }}
                }}
            }});
            """)
            
        html.append("</div></div>")

    html.append("</div>")
    html.append("<footer>Concurrency Performance Benchmark · Comparison Report</footer>")
    html.append("</div>")
    html.append("<script>")
    html.append("Chart.defaults.color = '#94a3b8';")
    html.extend(scripts)
    html.append("</script>")
    html.append("</body></html>")
    
    out_path = os.path.join(output_dir, "comparison-report.html")
    with open(out_path, 'w') as f:
        f.write("\n".join(html))
    return out_path


def generate_single_language_report(data, lang_name, output_dir):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    tasks = {t['taskName']: {cl['concurrency']: cl for cl in t['concurrencyLevels']} for t in data['tasks']} if data else {}
    all_task_names = sorted(tasks.keys())
    
    accent_color = "#f89820" if lang_name.lower() == "java" else "#3776ab"
    bg_color = "rgba(248, 152, 32, 0.1)" if lang_name.lower() == "java" else "rgba(55, 118, 171, 0.1)"
    
    html = [
        "<!DOCTYPE html>",
        "<html lang='en'>",
        "<head>",
        "    <meta charset='UTF-8'>",
        "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>",
        f"    <title>{lang_name} Performance Report</title>",
        "    <script src='https://cdn.jsdelivr.net/npm/chart.js'></script>",
        "    <style>",
        "        :root {",
        "            --primary: #1a1a2e;",
        "            --secondary: #16213e;",
        f"            --accent: {accent_color};",
        "            --text: #e2e8f0;",
        "            --bg: #0a0a0f;",
        "        }",
        "        * { box-sizing: border-box; margin: 0; padding: 0; }",
        "        body { font-family: 'Inter', system-ui, sans-serif; background: var(--bg); color: var(--text); line-height: 1.6; }",
        "        .container { max-width: 1200px; margin: 0 auto; padding: 40px 20px; }",
        "        header { ",
        "            background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);",
        "            padding: 50px; border-radius: 20px; margin-bottom: 40px; text-align: center;",
        "            box-shadow: 0 10px 30px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.05);",
        "        }",
        "        h1 { font-size: 2.5rem; margin-bottom: 10px; background: linear-gradient(to right, #fff, #94a3b8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }",
        "        .subtitle { color: #94a3b8; font-size: 1.1rem; }",
        "        .timestamp { display: inline-block; margin-top: 20px; padding: 5px 15px; background: rgba(255,255,255,0.05); border-radius: 20px; font-size: 0.85rem; }",
        "        ",
        "        .comparison-grid { display: grid; grid-template-columns: 1fr; gap: 40px; }",
        "        .task-card { ",
        "            background: #151521; border-radius: 16px; padding: 30px; border: 1px solid rgba(255,255,255,0.05);",
        "        }",
        "        h2 { font-size: 1.4rem; margin-bottom: 25px; color: #fff; display: flex; align-items: center; }",
        "        h2::before { content: ''; display: inline-block; width: 4px; height: 24px; background: var(--accent); margin-right: 12px; border-radius: 2px; }",
        "        ",
        "        .charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }",
        "        .chart-container { background: rgba(0,0,0,0.2); padding: 15px; border-radius: 8px; }",
        "        canvas { width: 100% !important; height: 300px !important; }",
        "        ",
        "        footer { text-align: center; margin-top: 60px; color: #475569; font-size: 0.9rem; }",
        "    </style>",
        "</head>",
        "<body>",
        "    <div class='container'>",
        "        <header>",
        f"            <h1>{lang_name} Performance Report</h1>",
        "            <div class='subtitle'>Concurrent Execution Scaling</div>",
        "            <div class='timestamp'>Generated: " + timestamp + "</div>",
        "        </header>",
        "        <div class='comparison-grid'>"
    ]

    scripts = []

    for idx, task_name in enumerate(all_task_names):
        levels = tasks.get(task_name, {})
        all_concurrencies = sorted(levels.keys())
        
        tp, lat, cpu, mem = [], [], [], []
        labels = []

        for conc in all_concurrencies:
            labels.append(str(conc))
            c_data = levels.get(conc)
            tp.append(c_data['throughputOpsPerSec'] if c_data else None)
            lat.append(c_data['avgLatencyMs'] if c_data else None)
            cpu.append(c_data['avgCpuPercent'] if c_data else None)
            mem.append(c_data['peakMemoryMB'] if c_data else None)

        task_id = f"task_{idx}"
        
        html.append(f"<div class='task-card'>")
        html.append(f"<h2>{task_name}</h2>")
        html.append("<div class='charts-grid'>")
        
        metrics = [
            ("Throughput (ops/s)", "tp", tp),
            ("Avg Latency (ms)", "lat", lat),
            ("CPU Usage (%)", "cpu", cpu),
            ("Peak Memory (MB)", "mem", mem)
        ]

        for m_name, m_id, m_data in metrics:
            cid = f"chart_{task_id}_{m_id}"
            html.append(f"<div class='chart-container'><canvas id='{cid}'></canvas></div>")
            
            scripts.append(f"""
            new Chart(document.getElementById('{cid}').getContext('2d'), {{
                type: 'line',
                data: {{
                    labels: {json.dumps(labels)},
                    datasets: [
                        {{ label: '{lang_name}', data: {json.dumps(m_data)}, borderColor: '{accent_color}', backgroundColor: '{bg_color}', tension: 0.1, fill: true }}
                    ]
                }},
                options: {{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {{
                        title: {{ display: true, text: '{m_name}', color: '#e2e8f0' }},
                        legend: {{ labels: {{ color: '#94a3b8' }} }}
                    }},
                    scales: {{
                        x: {{ title: {{ display: true, text: 'Threads', color: '#94a3b8' }}, ticks: {{ color: '#94a3b8' }}, grid: {{ color: 'rgba(255,255,255,0.05)' }} }},
                        y: {{ ticks: {{ color: '#94a3b8' }}, grid: {{ color: 'rgba(255,255,255,0.05)' }} }}
                    }}
                }}
            }});
            """)
            
        html.append("</div></div>")

    html.append("</div>")
    html.append(f"<footer>{lang_name} Concurrency Performance Benchmark</footer>")
    html.append("</div>")
    html.append("<script>")
    html.append("Chart.defaults.color = '#94a3b8';")
    html.extend(scripts)
    html.append("</script>")
    html.append("</body></html>")
    
    out_path = os.path.join(output_dir, f"{lang_name.lower()}-report.html")
    with open(out_path, 'w') as f:
        f.write("\n".join(html))
    return out_path

if __name__ == "__main__":
    results_dir = "benchmark-results"
    java_file = os.path.join(results_dir, "benchmark-results-java.json")
    python_file = os.path.join(results_dir, "benchmark-results-python.json")
    
    java_data = load_results(java_file)
    python_data = load_results(python_file)
    
    if not java_data and not python_data:
        print("Error: No result files found.")
    else:
        if java_data and python_data:
            report_path = generate_comparison_report(java_data, python_data, results_dir)
            print(f"Comparison report generated: {report_path}")
            
        if java_data:
            java_path = generate_single_language_report(java_data, "Java", results_dir)
            print(f"Java report generated: {java_path}")
            
        if python_data:
            python_path = generate_single_language_report(python_data, "Python", results_dir)
            print(f"Python report generated: {python_path}")
