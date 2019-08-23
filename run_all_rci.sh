sbatch --mem=30G -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih.log /home/fiedlda1/Amodsim/rci_launchers/ih.sh
sbatch --mem=30G -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-sw.log /home/fiedlda1/Amodsim/rci_launchers/ih-sw.sh
sbatch --mem=30G -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr.log /home/fiedlda1/Amodsim/rci_launchers/ih-nr.sh
sbatch --mem=30G -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr-sw.log /home/fiedlda1/Amodsim/rci_launchers/ih-nr-sw.sh
sbatch --mem=75G -t 1500 -n30 -p longjobs -o /home/fiedlda1/Amodsim/log/vga.log /home/fiedlda1/Amodsim/rci_launchers/vga.sh
sbatch --mem=75G -t 1500 -n30 -p longjobs -o /home/fiedlda1/Amodsim/log/vga-sw.log /home/fiedlda1/Amodsim/rci_launchers/vga-sw.sh
sbatch --mem=75G -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim.sh
sbatch --mem=75G -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim-sw.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim-sw.sh